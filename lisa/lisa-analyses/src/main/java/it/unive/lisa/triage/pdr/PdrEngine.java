package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.triage.bmc.EvaluatedFunctions;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.encoding.ExpressionEncoderFactory;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.BadCase;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.FreshValue;
import it.unive.lisa.triage.model.HeapModel;
import it.unive.lisa.triage.model.HeapOperators;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.ModelLocation;
import it.unive.lisa.triage.model.SymbolicCase;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.TypeSystem;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Property-directed reachability (IC3/PDR) over an {@link AlarmModel}, with
 * frames kept per location ({@link Frames}). Frame {@code F_0} is the set of
 * initial states, while every frame {@code F_i(l)}, with {@code i >= 1}, is the
 * conjunction of the invariant computed by the analysis at {@code l} and of the
 * lemmas of {@code l} with level at least {@code i}.<br>
 * <br>
 * At each level {@code k}, the engine looks for states of {@code F_k} at the
 * bad location that violate the property, and turns each of them into an
 * {@link Obligation}. Obligations are processed lowest level first: an
 * obligation {@code (l, c, i)} is blocked if no case of a transition leads from
 * {@code F_{i-1}} to {@code c} at {@code l} (and, if {@code l} is initial,
 * {@code c} contains no initial state), otherwise the predecessor found becomes
 * a new obligation at level {@code i - 1}. Predecessors are symbolic: they are
 * computed by model-based projection ({@link Projection}) of the cube through
 * the case of the transition taken in the model. An obligation at level
 * {@code 0} contains initial states, so the chain of obligations from it to the
 * bad location is a counterexample. Blocked cubes are first reduced to the
 * union of the unsat cores of the queries that blocked them (with no additional
 * query), then generalized by dropping literals, and their negation is added as
 * a lemma.<br>
 * <br>
 * Once every violation at level {@code k} is blocked, lemmas are propagated to
 * the next levels. If a level is left without lemmas of its own, two
 * consecutive frames coincide, and the frame is an inductive invariant: the
 * violation is impossible. This relies on the invariants of the analysis being
 * sound (they hold in every reachable state), while it does not require them to
 * be inductive with respect to the encoding of the transitions.<br>
 * <br>
 * Queries are two-state: the pre-state is step {@code 0} of an
 * {@link Unrolling}, the post-state is step {@code 1}. A single query covers
 * all the cases of all the transitions entering a location (the model tells
 * which one is taken), and the terms used by many queries (the invariants of
 * the analysis, the lemmas, the transitions and the initial condition) are sent
 * to the solver once, as definitions.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class PdrEngine {

	/**
	 * The outcome of a run of the engine.
	 */
	public enum Status {
		/**
		 * A chain of obligations from the initial states to the violation has
		 * been found.
		 */
		COUNTEREXAMPLE,

		/**
		 * An inductive invariant excluding the violation has been found.
		 */
		SAFE,

		/**
		 * No violation is possible within the reached level, but no inductive
		 * invariant has been found.
		 */
		BLOCKED,

		/**
		 * The solver could not decide a query.
		 */
		UNKNOWN,

		/**
		 * The maximum number of queries has been reached.
		 */
		BUDGET_EXHAUSTED
	}

	/**
	 * The strategies to generalize blocked cubes.
	 */
	public enum Generalization {
		/**
		 * Blocked cubes are not generalized.
		 */
		NONE,

		/**
		 * Blocked cubes are reduced to the unsat cores of the queries that
		 * blocked them, and then literals are dropped while the cube stays
		 * blocked.
		 */
		LITERALS,

		/**
		 * Blocked cubes are replaced by the proposal of the
		 * {@link CubeAbstraction}, if it is still blocked.
		 */
		DOMAIN,

		/**
		 * Blocked cubes are reduced to the unsat cores, replaced by the
		 * proposal of the {@link CubeAbstraction} if it is still blocked, and
		 * then literals are dropped.
		 */
		BOTH
	}

	private static final String CUBE = "#cube";

	/**
	 * The reason of imprecision of obligations whose cube still contains fresh
	 * values after the projection, that are thus left unconstrained.
	 */
	public static final String UNPROJECTED = "unprojected values: fresh values left unconstrained by the projection";

	/**
	 * The maximum size of the literals of cubes (the number of nodes of their
	 * trees): larger literals are dropped from predecessors, since literals
	 * grow with each pre-image.
	 */
	public static final long MAX_LITERAL_SIZE = 10_000;

	/**
	 * The imprecision of predecessors where conditions too large to be tracked
	 * have been dropped.
	 */
	public static final String OVERSIZED = "unprojected values: conditions too large to track";

	private Generalization generalization = Generalization.BOTH;

	private CubeAbstraction abstraction;

	private boolean seeding = true;

	private int proposals = 0;

	private int accepted = 0;

	/**
	 * The number of counterexamples to generalization blocked.
	 */
	private int ctgsBlocked = 0;

	/**
	 * Whether counterexamples to generalization are blocked (see
	 * {@link #blockedWithCtgs(ModelLocation, Cube, int, int)}).
	 */
	private boolean ctgs = false;

	/**
	 * Whether candidate invariants are checked before the search (see
	 * {@link #invariants()}).
	 */
	private boolean invariants = true;

	/**
	 * The number of candidate invariants proved inductive.
	 */
	private int invariantsFound = 0;

	/**
	 * The maximum number of locations with candidate invariants.
	 */
	public static final int MAX_CANDIDATE_LOCATIONS = 100;

	/**
	 * The maximum number of candidate invariants at each location.
	 */
	public static final int MAX_CANDIDATES = 60;

	/**
	 * The maximum number of variables of each location over which candidate
	 * invariants are built.
	 */
	public static final int MAX_CANDIDATE_VARIABLES = 8;

	/**
	 * The maximum number of constants used by candidate invariants.
	 */
	public static final int MAX_CANDIDATE_CONSTANTS = 6;

	/**
	 * The maximum number of queries spent checking candidate invariants.
	 */
	public static final int MAX_INVARIANT_QUERIES = 200;

	/**
	 * The maximum number of counterexamples to generalization blocked when
	 * trying to drop a literal from a cube.
	 */
	public static final int MAX_CTGS = 3;

	/**
	 * The maximum depth of the recursive generalization of the counterexamples
	 * to generalization.
	 */
	public static final int CTG_DEPTH = 1;

	private final AlarmModel model;

	private final SmtSolver solver;

	private final Unrolling unrolling;

	private final TypeSystem types;

	private final boolean strict;

	private final Frames<ModelLocation, Lemma> frames = new Frames<>();

	private final Map<ModelLocation, List<EdgeTransition>> incoming = new LinkedHashMap<>();

	private final List<Identifier> state = new ArrayList<>();

	private final PriorityQueue<Obligation> queue = new PriorityQueue<>();

	private long created = 0;

	private int queries = 0;

	private int maxQueries = Integer.MAX_VALUE;

	private long deadline = Long.MAX_VALUE;

	private long literalNames = 0;

	private List<Obligation> counterexample;

	/**
	 * The terms sent to the solver once, as definitions, since they are used by
	 * many queries: the seeds of each location, the initial condition, the
	 * lemmas and the transitions, at the steps they are used at.
	 */
	private final Map<Object, Term> definitions = new LinkedHashMap<>();

	private int invariantLevel = -1;

	/**
	 * Signals that the solver could not decide a query, or that the budget is
	 * exhausted.
	 */
	private static class Stop
			extends
			Exception {

		private static final long serialVersionUID = 1L;

		private final Status status;

		private Stop(
				Status status) {
			this.status = status;
		}
	}

	/**
	 * Builds the engine.
	 *
	 * @param model  the model to check
	 * @param solver the solver to use, that is not closed by the engine
	 */
	public PdrEngine(
			AlarmModel model,
			SmtSolver solver) {
		this(model, solver, false);
	}

	/**
	 * Builds the engine.
	 *
	 * @param model  the model to check
	 * @param solver the solver to use, that is not closed by the engine
	 * @param strict whether or not to encode the model in strict mode (see
	 *                   {@link Unrolling#Unrolling(AlarmModel, SmtSolver, boolean)}):
	 *                   in strict mode, counterexamples use only values that
	 *                   are computed precisely, but the absence of
	 *                   counterexamples does not prove anything
	 */
	public PdrEngine(
			AlarmModel model,
			SmtSolver solver,
			boolean strict) {
		this(model, solver, strict, ExpressionEncoderFactory.DEFAULT);
	}

	/**
	 * Builds the engine, as {@link #PdrEngine(AlarmModel, SmtSolver, boolean)},
	 * encoding expressions with the encoders built by the given factory (e.g.,
	 * encoders knowing the operators of a frontend).
	 *
	 * @param model    the model to check
	 * @param solver   the solver to use, that is not closed by the engine
	 * @param strict   whether or not to encode the model in strict mode
	 * @param encoders the factory of the encoders of expressions
	 */
	public PdrEngine(
			AlarmModel model,
			SmtSolver solver,
			boolean strict,
			ExpressionEncoderFactory encoders) {
		this.model = model;
		this.solver = solver;
		this.strict = strict;
		this.unrolling = new Unrolling(model, solver, strict, encoders);
		this.types = model.getBadLocation().getNode().getProgram().getTypes();
		for (ModelLocation loc : model.getLocations())
			for (EdgeTransition e : model.getTransitions(loc))
				incoming.computeIfAbsent(e.getDestination(), d -> new ArrayList<>()).add(e);
		for (Identifier id : model.getVocabulary())
			if (!(id instanceof FreshValue))
				state.add(id);
	}

	/**
	 * Sets the strategy to generalize blocked cubes (default:
	 * {@link Generalization#BOTH}).
	 *
	 * @param generalization the strategy
	 */
	public void setGeneralization(
			Generalization generalization) {
		this.generalization = generalization;
	}

	/**
	 * Sets the abstraction proposing generalizations of blocked cubes, used by
	 * {@link Generalization#DOMAIN} and {@link Generalization#BOTH} (default:
	 * none).
	 *
	 * @param abstraction the abstraction, or {@code null}
	 */
	public void setAbstraction(
			CubeAbstraction abstraction) {
		this.abstraction = abstraction;
	}

	/**
	 * Sets whether or not the generalization of blocked cubes blocks the
	 * counterexamples to generalization it finds (default: {@code false}): when
	 * dropping a literal from a cube fails because a state reaches the smaller
	 * cube, that state is blocked at the previous level (if it is not an
	 * initial state) and the literal is dropped again, as in IC3 with
	 * counterexamples to generalization.
	 *
	 * @param ctgs whether or not to block counterexamples to generalization
	 */
	public void setCtgs(
			boolean ctgs) {
		this.ctgs = ctgs;
	}

	/**
	 * Sets the known values of the functions that the encoder computes on
	 * concrete values (see {@link Unrolling#setEvaluatedFunctions}): the
	 * encodings give the functions those values, and the values learned from
	 * the models of the search are added to them, so that a following search
	 * can use them.
	 *
	 * @param evaluated the known values
	 */
	public void setEvaluatedFunctions(
			EvaluatedFunctions evaluated) {
		unrolling.setEvaluatedFunctions(evaluated);
	}

	/**
	 * Sets whether or not candidate invariants are checked before the search
	 * (default: {@code true}): simple relations between the integer variables
	 * at each location, and between them and the constants of the model, that
	 * are kept as lemmas if they are inductive (see {@link #invariants()}).
	 *
	 * @param invariants whether or not to check candidate invariants
	 */
	public void setInvariants(
			boolean invariants) {
		this.invariants = invariants;
	}

	/**
	 * Yields the number of candidate invariants found inductive so far (see
	 * {@link #setInvariants(boolean)}).
	 *
	 * @return the number of invariants
	 */
	public int getInvariantsFound() {
		return invariantsFound;
	}

	/**
	 * Yields the number of counterexamples to generalization blocked so far
	 * (see {@link #setCtgs(boolean)}).
	 *
	 * @return the number of counterexamples to generalization
	 */
	public int getCtgs() {
		return ctgsBlocked;
	}

	/**
	 * Sets whether or not frames include the invariants computed by the
	 * analysis (default: {@code true}). Without them, the engine behaves as
	 * plain property-directed reachability.
	 *
	 * @param seeding whether or not to use the invariants
	 */
	public void setSeeding(
			boolean seeding) {
		this.seeding = seeding;
	}

	/**
	 * Yields the number of generalizations proposed by the
	 * {@link CubeAbstraction} so far.
	 *
	 * @return the number of proposals
	 */
	public int getProposals() {
		return proposals;
	}

	/**
	 * Yields the number of generalizations proposed by the
	 * {@link CubeAbstraction} that have been used, since they were still
	 * blocked.
	 *
	 * @return the number of accepted proposals
	 */
	public int getAcceptedProposals() {
		return accepted;
	}

	/**
	 * Yields the frames.
	 *
	 * @return the frames
	 */
	public Frames<ModelLocation, Lemma> getFrames() {
		return frames;
	}

	/**
	 * Yields the unrolling used to encode queries.
	 *
	 * @return the unrolling
	 */
	public Unrolling getUnrolling() {
		return unrolling;
	}

	/**
	 * Why the solver could not decide the query that stopped the search, if
	 * any.
	 */
	private String reasonUnknown;

	/**
	 * Yields why the solver could not decide the query that stopped the search
	 * (with {@link Status#UNKNOWN}).
	 *
	 * @return the reason, or {@code null} if the search did not stop because of
	 *             an undecided query
	 */
	public String getReasonUnknown() {
		return reasonUnknown;
	}

	/**
	 * Stops the search after a query the solver could not decide, recording
	 * why: the reason is asked before the scope of the query is closed, as
	 * solvers forget it afterwards.
	 */
	private void unknown()
			throws Stop {
		try {
			reasonUnknown = solver.getReasonUnknown();
		} catch (SmtException e) {
			reasonUnknown = "unknown reason (" + e.getMessage() + ")";
		}
		solver.pop();
		throw new Stop(Status.UNKNOWN);
	}

	/**
	 * Yields the number of queries issued so far.
	 *
	 * @return the number of queries
	 */
	public int getQueries() {
		return queries;
	}

	/**
	 * Yields the counterexample found by the last run returning
	 * {@link Status#COUNTEREXAMPLE}: the chain of obligations from the one at
	 * level {@code 0} (containing initial states) to the one at the bad
	 * location.
	 *
	 * @return the counterexample, or {@code null}
	 */
	public List<Obligation> getCounterexample() {
		return counterexample;
	}

	/**
	 * Yields the inductive invariant found by the last run returning
	 * {@link Status#SAFE}: the lemmas of each location, that hold together with
	 * the invariants computed by the analysis.
	 *
	 * @return the invariant, or {@code null}
	 */
	public Map<ModelLocation, Set<Lemma>> getInvariant() {
		return invariantLevel < 0 ? null : frames.invariant(invariantLevel);
	}

	/**
	 * Runs the engine until a counterexample or an inductive invariant is
	 * found, or every violation within {@code maxLevel} steps has been blocked,
	 * with no time limit.
	 *
	 * @param maxLevel   the maximum level to reach
	 * @param maxQueries the maximum number of queries to issue
	 *
	 * @return the outcome
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Status run(
			int maxLevel,
			int maxQueries)
			throws SemanticException {
		return run(maxLevel, maxQueries, 0);
	}

	/**
	 * Runs the engine until a counterexample or an inductive invariant is
	 * found, or every violation within {@code maxLevel} steps has been blocked.
	 * The time limit is checked before each query: a single query is bounded by
	 * the timeout of the solver.
	 *
	 * @param maxLevel   the maximum level to reach
	 * @param maxQueries the maximum number of queries to issue
	 * @param maxMillis  the maximum time to spend, in milliseconds ({@code 0}
	 *                       or negative means no limit)
	 *
	 * @return the outcome
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public Status run(
			int maxLevel,
			int maxQueries,
			long maxMillis)
			throws SemanticException {
		this.maxQueries = maxQueries;
		this.deadline = maxMillis > 0 ? System.nanoTime() + maxMillis * 1_000_000 : Long.MAX_VALUE;
		try {
			// violations in the initial states
			Obligation bad = badObligation(0);
			if (bad != null) {
				counterexample = List.of(bad);
				return Status.COUNTEREXAMPLE;
			}
			if (invariants)
				invariants();

			for (int k = 1; k <= maxLevel; k++) {
				while ((bad = badObligation(k)) != null) {
					queue.add(bad);
					if (blockAll())
						return Status.COUNTEREXAMPLE;
				}
				frames.extend();
				propagate(k);
				int converged = frames.convergedLevel();
				if (converged > 0) {
					invariantLevel = converged;
					return Status.SAFE;
				}
			}
			return Status.BLOCKED;
		} catch (Stop s) {
			queue.clear();
			return s.status;
		}
	}

	/**
	 * Checks candidate invariants, in the style of Houdini: at each location,
	 * the candidates are the equalities and inequalities between the integer
	 * variables relevant at the location, and between them and the integer
	 * constants of the model. Candidates that do not hold in the initial
	 * states, or that are not preserved by the transitions reaching their
	 * location (assuming all the remaining candidates at the sources, together
	 * with the invariants computed by the analysis), are dropped until the
	 * remaining ones are inductive: they hold in every reachable state, and are
	 * added as lemmas of every level. If the candidates cannot be decided
	 * within {@link #MAX_INVARIANT_QUERIES} queries, none is kept.
	 */
	private void invariants()
			throws SemanticException,
			Stop {
		Map<ModelLocation, Map<Lemma, Term>> candidates = candidates();
		if (candidates.isEmpty())
			return;
		int start = queries;
		try {
			boolean changed = true;
			while (changed) {
				changed = false;
				for (Map.Entry<ModelLocation, Map<Lemma, Term>> entry : candidates.entrySet()) {
					ModelLocation loc = entry.getKey();
					Map<Lemma, Term> current = entry.getValue();
					if (current.isEmpty())
						continue;
					if (model.getInitialLocations().contains(loc) && !holdInitially(loc, current, start)) {
						changed = true;
						if (current.isEmpty())
							continue;
					}
					List<Term> steps = new ArrayList<>();
					for (EdgeTransition e : incoming.getOrDefault(loc, Collections.emptyList())) {
						ModelLocation src = e.getSource();
						if (!model.getLocations().contains(src))
							continue;
						List<Term> pre = new ArrayList<>();
						pre.add(frame(src, 1));
						Map<Lemma, Term> assumed = candidates.get(src);
						if (assumed != null)
							for (Lemma l : assumed.keySet())
								pre.add(define(List.of(l, 0), () -> l.at(unrolling, 0)));
						pre.add(define(List.of(e), () -> unrolling.transition(0, e)));
						steps.add(Term.and(pre.toArray(Term[]::new)));
					}
					if (steps.isEmpty())
						continue;
					// the candidates after a step
					Map<Lemma, Term> post = new LinkedHashMap<>();
					for (Lemma l : current.keySet())
						post.put(l, define(List.of(l, 1), () -> l.at(unrolling, 1)));
					while (!current.isEmpty()) {
						if (queries - start >= MAX_INVARIANT_QUERIES)
							return;
						List<Term> violated = new ArrayList<>();
						for (Lemma l : current.keySet())
							violated.add(Term.not(post.get(l)));
						SmtResult res = check(Term.or(steps.toArray(Term[]::new)),
								Term.or(violated.toArray(Term[]::new)));
						if (res != SmtResult.SAT) {
							solver.pop();
							break;
						}
						// the candidates false in the model are dropped
						List<Term> asked = new ArrayList<>();
						for (Lemma l : current.keySet())
							asked.add(post.get(l));
						Map<Term, String> values = solver.getValues(asked);
						solver.pop();
						int before = current.size();
						current.keySet().removeIf(l -> "false".equals(values.get(post.get(l))));
						if (current.size() == before)
							// the model does not tell which candidate is
							// violated: all are dropped
							current.clear();
						changed = true;
					}
				}
			}
		} catch (Stop s) {
			if (s.status != Status.UNKNOWN)
				throw s;
			// the candidates cannot be decided: none is kept
			return;
		}
		for (Map.Entry<ModelLocation, Map<Lemma, Term>> entry : candidates.entrySet())
			for (Lemma l : entry.getValue().keySet()) {
				frames.add(entry.getKey(), l, Frames.INFINITY);
				invariantsFound++;
			}
	}

	/**
	 * Drops the given candidates of the given initial location that do not hold
	 * in its initial states, yielding whether all of them hold.
	 *
	 * @throws Stop if the budget is exhausted or the solver cannot decide
	 */
	private boolean holdInitially(
			ModelLocation loc,
			Map<Lemma, Term> current,
			int start)
			throws SemanticException,
			Stop {
		boolean held = true;
		Map<Lemma, Term> initial = new LinkedHashMap<>();
		for (Lemma l : current.keySet())
			initial.put(l, define(List.of(l, 0), () -> l.at(unrolling, 0)));
		while (!current.isEmpty() && queries - start < MAX_INVARIANT_QUERIES) {
			List<Term> violated = new ArrayList<>();
			for (Lemma l : current.keySet())
				violated.add(Term.not(initial.get(l)));
			SmtResult res = check(frame(loc, 0), Term.or(violated.toArray(Term[]::new)));
			if (res != SmtResult.SAT) {
				solver.pop();
				return held;
			}
			List<Term> asked = new ArrayList<>();
			for (Lemma l : current.keySet())
				asked.add(initial.get(l));
			Map<Term, String> values = solver.getValues(asked);
			solver.pop();
			int before = current.size();
			current.keySet().removeIf(l -> "false".equals(values.get(initial.get(l))));
			if (current.size() == before)
				current.clear();
			held = false;
		}
		return held;
	}

	/**
	 * Yields the candidate invariants of each location (see
	 * {@link #invariants()}).
	 */
	private Map<ModelLocation, Map<Lemma, Term>> candidates() {
		// the integer constants and the variables compared by the model
		Set<Integer> constants = new LinkedHashSet<>();
		Set<Identifier> relevant = new LinkedHashSet<>();
		for (ModelLocation loc : model.getLocations())
			for (EdgeTransition e : model.getTransitions(loc))
				for (SymbolicCase c : unrolling.cases(e))
					for (SymbolicExpression cond : c.getPathCondition())
						compared(cond, constants, relevant);
		for (BadCase bad : model.getBadCases())
			for (SymbolicExpression cond : bad.getPathCondition())
				compared(cond, constants, relevant);
		List<SymbolicExpression> numbers = new ArrayList<>();
		for (Integer c : constants)
			if (numbers.size() < MAX_CANDIDATE_CONSTANTS)
				numbers.add(new Constant(types.getIntegerType(), c, SyntheticLocation.INSTANCE));

		Map<ModelLocation, Map<Lemma, Term>> result = new LinkedHashMap<>();
		for (ModelLocation loc : model.getLocations()) {
			if (result.size() >= MAX_CANDIDATE_LOCATIONS)
				break;
			List<Identifier> vars = new ArrayList<>();
			for (Identifier id : model.getLive(loc))
				if (relevant.contains(id) && vars.size() < MAX_CANDIDATE_VARIABLES)
					vars.add(id);
			Map<Lemma, Term> cands = new LinkedHashMap<>();
			for (int i = 0; i < vars.size(); i++) {
				for (int j = i + 1; j < vars.size(); j++) {
					candidate(cands, vars.get(i), vars.get(j), ComparisonEq.INSTANCE);
					candidate(cands, vars.get(i), vars.get(j), ComparisonLe.INSTANCE);
					candidate(cands, vars.get(i), vars.get(j), ComparisonGe.INSTANCE);
				}
				for (SymbolicExpression n : numbers) {
					candidate(cands, vars.get(i), n, ComparisonLe.INSTANCE);
					candidate(cands, vars.get(i), n, ComparisonGe.INSTANCE);
				}
			}
			if (!cands.isEmpty())
				result.put(loc, cands);
		}
		return result;
	}

	/**
	 * Adds to the given candidates the lemma stating that the given expressions
	 * are in the given relation, if there is room.
	 */
	private void candidate(
			Map<Lemma, Term> candidates,
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator op) {
		if (candidates.size() >= MAX_CANDIDATES)
			return;
		SymbolicExpression relation = new BinaryExpression(types.getBooleanType(), left, right, op,
				SyntheticLocation.INSTANCE);
		// the lemma blocking the states where the relation does not hold
		SymbolicExpression negated = new UnaryExpression(types.getBooleanType(), relation,
				LogicalNegation.INSTANCE, SyntheticLocation.INSTANCE);
		candidates.put(new Cube(List.of(negated)).negate(), null);
	}

	/**
	 * Collects the integer constants and the integer variables of the model
	 * compared by the given condition.
	 */
	private void compared(
			SymbolicExpression cond,
			Set<Integer> constants,
			Set<Identifier> variables) {
		if (cond instanceof UnaryExpression) {
			compared(((UnaryExpression) cond).getExpression(), constants, variables);
			return;
		}
		if (!(cond instanceof BinaryExpression))
			return;
		BinaryExpression b = (BinaryExpression) cond;
		if (b.getOperator() instanceof LogicalAnd || b.getOperator() instanceof LogicalOr) {
			compared(b.getLeft(), constants, variables);
			compared(b.getRight(), constants, variables);
			return;
		}
		if (!(b.getOperator() instanceof ComparisonOperator))
			return;
		for (SymbolicExpression operand : List.of(b.getLeft(), b.getRight()))
			if (operand instanceof Constant && ((Constant) operand).getValue() instanceof Integer)
				constants.add((Integer) ((Constant) operand).getValue());
			else if (operand instanceof Identifier && !(operand instanceof FreshValue)
					&& model.getVocabulary().contains(operand)
					&& model.getSorts().sortOf((Identifier) operand).equals(Sort.INT)
					&& !HeapModel.isHeapState(operand))
				variables.add((Identifier) operand);
	}

	/**
	 * Yields an obligation for a state of {@code F_level} (or of the initial
	 * states, if {@code level} is {@code 0}) at the bad location, violating the
	 * property, or {@code null} if there is none.
	 */
	private Obligation badObligation(
			int level)
			throws SemanticException,
			Stop {
		ModelLocation bad = model.getBadLocation();
		for (BadCase c : model.getBadCases()) {
			List<SymbolicExpression> lits = new ArrayList<>(c.getPathCondition());
			if (c.getViolation() != null)
				lits.add(c.getViolation());
			lits.addAll(written(lits));
			Cube cube = new Cube(lits);
			Term query = cube.at(unrolling, 0, CUBE);
			String key = "cond@0" + CUBE;
			if (check(frame(bad, level), query) == SmtResult.SAT) {
				Set<String> imprecisions = new LinkedHashSet<>(unrolling.getImprecisions(key, solver));
				Map<FreshValue, String> values = freshValues(cube.getLiterals(), key);
				Cube projected = project(cube.getLiterals(), values);
				if (!Projection.freshValues(projected.getLiterals()).isEmpty())
					imprecisions.add(UNPROJECTED);
				// the values of the state are only needed for initial states
				Obligation o = new Obligation(bad, projected, level, null, null, inputs(values), imprecisions,
						level == 0 ? point() : Collections.emptyMap(), created++);
				solver.pop();
				return o;
			}
			solver.pop();
		}
		return null;
	}

	/**
	 * Processes the queue until it is empty (every obligation is blocked) or a
	 * counterexample is found.
	 *
	 * @return {@code true} if a counterexample has been found
	 */
	private boolean blockAll()
			throws SemanticException,
			Stop {
		while (!queue.isEmpty()) {
			Obligation o = queue.poll();
			if (o.getLevel() == 0) {
				counterexample = new ArrayList<>();
				for (Obligation cur = o; cur != null; cur = cur.getParent())
					counterexample.add(cur);
				queue.clear();
				return true;
			}
			// already blocked by a lemma found meanwhile
			SmtResult res = check(frame(o.getLocation(), o.getLevel()), o.getCube().at(unrolling, 0, CUBE));
			solver.pop();
			if (res == SmtResult.UNSAT)
				continue;

			Set<SymbolicExpression> core = new LinkedHashSet<>();
			Obligation pred = predecessor(o.getLocation(), o.getCube(), o.getLevel(), o, core);
			if (pred != null) {
				queue.add(pred);
				queue.add(o);
				continue;
			}

			Cube cube = generalize(o.getLocation(), o.getCube(), core, o.getLevel());
			frames.add(o.getLocation(), cube.negate(), o.getLevel());
			// the same states might still reach the bad location in more steps
			if (o.getLevel() < frames.depth())
				queue.add(new Obligation(o.getLocation(), o.getCube(), o.getLevel() + 1, o.getParent(),
						o.getEdge(), o.getInputs(), o.getImprecisions(), o.getPoint(), created++));
		}
		return false;
	}

	/**
	 * Yields a predecessor of the given cube at the given location: an initial
	 * state in the cube (as an obligation at level {@code 0}, with no
	 * transition), or the projection of the cube on a state of
	 * {@code F_{level-1}} at another location that reaches the cube in one
	 * transition. Yields {@code null} if there is none (the cube is blocked at
	 * the given level): in that case, if {@code core} is not {@code null}, it
	 * is filled with literals of the cube that are enough to block it (the
	 * union of the unsat cores of the queries).
	 */
	private Obligation predecessor(
			ModelLocation location,
			Cube cube,
			int level,
			Obligation parent,
			Set<SymbolicExpression> core)
			throws SemanticException,
			Stop {
		if (model.getInitialLocations().contains(location)) {
			if (check(List.of(frame(location, 0)), cube, 0, core) == SmtResult.SAT) {
				// no step is taken: the obligation refines its parent
				Obligation init = new Obligation(location, cube, 0, parent, null, Collections.emptyMap(),
						Collections.emptySet(), point(), created++);
				solver.pop();
				return init;
			}
			solver.pop();
		}
		// a single query for all the cases of all the transitions entering the
		// location, whose model tells which one is taken
		List<EdgeTransition> edges = new ArrayList<>();
		List<Integer> indexes = new ArrayList<>();
		List<Term> steps = new ArrayList<>();
		for (EdgeTransition e : incoming.getOrDefault(location, Collections.emptyList())) {
			ModelLocation src = e.getSource();
			if (!model.getLocations().contains(src))
				continue;
			Term pre = frame(src, level - 1);
			for (int i = 0; i < unrolling.cases(e).size(); i++) {
				edges.add(e);
				indexes.add(i);
				steps.add(Term.and(pre, transition(e, i)));
			}
		}
		if (steps.isEmpty())
			return null;
		if (check(List.of(Term.or(steps.toArray(Term[]::new))), cube, 1, core) != SmtResult.SAT) {
			solver.pop();
			return null;
		}
		int taken = 0;
		if (steps.size() > 1) {
			Map<Term, String> holding = solver.getValues(steps);
			while (taken < steps.size() - 1 && !"true".equals(holding.get(steps.get(taken))))
				taken++;
		}
		EdgeTransition e = edges.get(taken);
		int i = indexes.get(taken);
		ModelLocation src = e.getSource();
		String caseKey = unrolling.caseKey(0, e, i);
		String cubeKey = "cond@1" + CUBE;
		// the cube is encoded as a whole (literals might have been encoded one
		// by one) to compute its exactness in the model
		cube.at(unrolling, 1, CUBE);
		Set<String> imprecisions = new LinkedHashSet<>(unrolling.getImprecisions(caseKey, solver));
		imprecisions.addAll(unrolling.getImprecisions(cubeKey, solver));
		SymbolicCase c = unrolling.cases(e).get(i);
		List<SymbolicExpression> lits = new ArrayList<>(c.getPathCondition());
		for (SymbolicExpression lit : cube.getLiterals()) {
			SymbolicExpression image = c.substitute(lit, e.getStore().getFactory(), null);
			if (SymbolicCase.isTooLarge(image, MAX_LITERAL_SIZE))
				// literals grow with each pre-image: dropping one enlarges
				// the predecessor, that is still sound for blocking, but not
				// for confirming
				imprecisions.add(OVERSIZED);
			else
				lits.add(image);
		}
		List<SymbolicExpression> read = new ArrayList<>(lits);
		for (Map.Entry<Identifier, SymbolicExpression> update : c.getUpdates().entrySet())
			if (model.getVocabulary().contains(update.getKey()))
				read.add(update.getValue());
		lits.addAll(written(read));
		Map<FreshValue, String> values = freshValues(lits, caseKey);
		// fresh values of the cube that survived substitution
		Map<FreshValue, String> fromCube = freshValues(lits, cubeKey);
		fromCube.keySet().removeAll(values.keySet());
		Map<FreshValue, String> all = new LinkedHashMap<>(values);
		all.putAll(fromCube);
		// every input of the case, even if not in the cube, is needed to
		// reproduce the transition, except for the ones that the executions
		// of the case do not consume, while the ones of other cases of the
		// same transition do (e.g., the result of the right operand of a
		// short-circuiting operator, not evaluated in this case)
		Map<FreshValue, String> caseInputs = freshValues(unrolling.getFreshValues(caseKey), caseKey);
		Set<InputValue> producedElsewhere = new HashSet<>();
		for (SymbolicCase other : unrolling.cases(e))
			producedElsewhere.addAll(other.getProduced());
		producedElsewhere.removeAll(c.getProduced());
		caseInputs.keySet().removeAll(producedElsewhere);
		Cube projected = project(lits, all);
		// values that could not be projected are left unconstrained
		if (!Projection.freshValues(projected.getLiterals()).isEmpty())
			imprecisions.add(UNPROJECTED);
		// the values of the state are only needed for initial states
		Obligation pred = new Obligation(src, projected, level - 1, parent, e, inputs(caseInputs),
				imprecisions, level - 1 == 0 ? point() : Collections.emptyMap(), created++);
		solver.pop();
		return pred;
	}

	/**
	 * In strict mode, yields the conditions stating that the heap locations
	 * read by the given expressions have been written (see
	 * {@link HeapOperators.WrittenAt}), that are added to the obligations: a
	 * read is precise only if its location has been written, and this is a
	 * property of the whole execution reaching the read, not only of the step
	 * performing it. Adding the conditions to the obligations makes the
	 * predecessors computed for them satisfy them too, until the writes are
	 * reached. Strict queries require the reads to be precise, so the
	 * conditions hold in the models of the queries yielding the obligations.
	 * Outside strict mode, no condition is yielded.
	 */
	private List<SymbolicExpression> written(
			Collection<SymbolicExpression> expressions) {
		if (!strict)
			return List.of();
		Set<SymbolicExpression> result = new LinkedHashSet<>();
		for (SymbolicExpression e : expressions)
			written(e, result);
		return new ArrayList<>(result);
	}

	private void written(
			SymbolicExpression e,
			Set<SymbolicExpression> result) {
		if (e instanceof UnaryExpression)
			written(((UnaryExpression) e).getExpression(), result);
		else if (e instanceof BinaryExpression) {
			written(((BinaryExpression) e).getLeft(), result);
			written(((BinaryExpression) e).getRight(), result);
		} else if (e instanceof TernaryExpression) {
			TernaryExpression t = (TernaryExpression) e;
			written(t.getLeft(), result);
			written(t.getMiddle(), result);
			written(t.getRight(), result);
			if (t.getOperator() instanceof HeapOperators.ReferenceRead)
				result.add(new BinaryExpression(types.getBooleanType(), t.getMiddle(), t.getRight(),
						HeapOperators.WrittenAt.INSTANCE, t.getCodeLocation()));
		}
	}

	/**
	 * Yields the relation of the case with the given index of the given
	 * transition, from step {@code 0} to step {@code 1}, sent to the solver
	 * once (see {@link Unrolling#transition(int, EdgeTransition, int)}).
	 */
	private Term transition(
			EdgeTransition edge,
			int index)
			throws SemanticException {
		return define(List.of(edge, index), () -> unrolling.transition(0, edge, index));
	}

	/**
	 * Yields the given term, sent to the solver once as a definition, and
	 * identified by the given key.
	 */
	private Term define(
			Object key,
			Encoding encoding)
			throws SemanticException {
		Term defined = definitions.get(key);
		if (defined == null) {
			Term body = encoding.encode();
			defined = body.equals(Term.TRUE) || body.equals(Term.FALSE) ? body
					: solver.define("def!" + definitions.size(), body);
			definitions.put(key, defined);
		}
		return defined;
	}

	/**
	 * The encoding of a term.
	 */
	@FunctionalInterface
	private interface Encoding {

		Term encode()
				throws SemanticException;
	}

	/**
	 * Generalizes a cube blocked at the given level, according to the
	 * configured {@link Generalization}, given the union of the unsat cores of
	 * the queries that blocked it.
	 */
	private Cube generalize(
			ModelLocation location,
			Cube cube,
			Set<SymbolicExpression> core,
			int level)
			throws SemanticException,
			Stop {
		if (generalization == Generalization.NONE)
			return cube;
		Cube current = generalization == Generalization.DOMAIN ? cube : cube.restrict(core);
		if (abstraction != null
				&& (generalization == Generalization.DOMAIN || generalization == Generalization.BOTH)) {
			Cube proposal = abstraction.abstractCube(location, current);
			if (proposal != null && !proposal.equals(current)) {
				proposals++;
				if (predecessor(location, proposal, level, null, null) == null) {
					accepted++;
					current = proposal;
				}
			}
		}
		if (generalization == Generalization.DOMAIN)
			return current;
		for (SymbolicExpression lit : current.getLiterals()) {
			Cube candidate = current.without(lit);
			if (blockedWithCtgs(location, candidate, level, CTG_DEPTH))
				current = candidate;
		}
		return current;
	}

	/**
	 * Yields whether the given cube is blocked at the given level, trying to
	 * block the states that prevent it (counterexamples to generalization,
	 * CTGs): when a predecessor of the cube exists, but is not an initial
	 * state, it is blocked at the previous level, if possible (generalizing it
	 * and adding its lemma), and the cube is checked again, up to
	 * {@link #MAX_CTGS} times. The lemmas added this way are relatively
	 * inductive, so they hold whatever the outcome.
	 */
	private boolean blockedWithCtgs(
			ModelLocation location,
			Cube cube,
			int level,
			int depth)
			throws SemanticException,
			Stop {
		for (int ctgs = 0;; ctgs++) {
			Obligation ctg = predecessor(location, cube, level, null, null);
			if (ctg == null)
				return true;
			if (!this.ctgs || depth == 0 || ctgs >= MAX_CTGS || ctg.getLevel() == 0 || ctg.getEdge() == null)
				return false;
			ModelLocation src = ctg.getLocation();
			Set<SymbolicExpression> core = new LinkedHashSet<>();
			if (predecessor(src, ctg.getCube(), ctg.getLevel(), null, core) != null)
				// the counterexample to generalization cannot be blocked
				return false;
			Cube blocked = ctg.getCube().restrict(core);
			for (SymbolicExpression lit : blocked.getLiterals()) {
				Cube candidate = blocked.without(lit);
				if (blockedWithCtgs(src, candidate, ctg.getLevel(), depth - 1))
					blocked = candidate;
			}
			frames.add(src, blocked.negate(), ctg.getLevel());
			ctgsBlocked++;
		}
	}

	/**
	 * Pushes the lemmas of each level up to {@code k} to the next level, when
	 * they are preserved by every transition entering their location.
	 */
	private void propagate(
			int k)
			throws SemanticException,
			Stop {
		for (int i = 1; i <= k; i++)
			for (ModelLocation loc : model.getLocations())
				for (Lemma lemma : frames.exactly(loc, i))
					if (preserved(loc, lemma, i))
						frames.add(loc, lemma, i + 1);
	}

	private boolean preserved(
			ModelLocation location,
			Lemma lemma,
			int level)
			throws SemanticException,
			Stop {
		List<Term> steps = new ArrayList<>();
		for (EdgeTransition e : incoming.getOrDefault(location, Collections.emptyList())) {
			ModelLocation src = e.getSource();
			if (!model.getLocations().contains(src))
				continue;
			steps.add(Term.and(frame(src, level),
					define(List.of(e), () -> unrolling.transition(0, e))));
		}
		if (steps.isEmpty())
			return true;
		Term post = Term.not(define(List.of(lemma, 1), () -> lemma.at(unrolling, 1)));
		SmtResult res = check(Term.or(steps.toArray(Term[]::new)), post);
		solver.pop();
		return res == SmtResult.UNSAT;
	}

	/**
	 * Yields frame {@code F_level} at the given location, over step {@code 0},
	 * including the program counter.
	 */
	private Term frame(
			ModelLocation location,
			int level)
			throws SemanticException {
		if (level == 0)
			return Term.and(unrolling.at(0, location), define("init", () -> unrolling.initial()));
		List<Term> conj = new ArrayList<>();
		conj.add(unrolling.at(0, location));
		if (seeding)
			conj.add(define(location, () -> unrolling.seeds(location, 0)));
		for (Lemma lemma : frames.frame(location, level))
			conj.add(define(List.of(lemma, 0), () -> lemma.at(unrolling, 0)));
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Checks the conjunction of the given terms in a new scope, that is left
	 * open (so that the model can be read) and must be closed by the caller.
	 *
	 * @throws Stop if the budget is exhausted or the solver cannot decide (in
	 *                  which case the scope is closed)
	 */
	private SmtResult check(
			Term... terms)
			throws Stop {
		budget();
		queries++;
		solver.push();
		for (Term t : terms)
			solver.assertTerm(t);
		SmtResult res = solver.check();
		if (res == SmtResult.UNKNOWN)
			unknown();
		return res;
	}

	/**
	 * Checks the conjunction of the given terms and of the given cube at the
	 * given step, like {@link #check(Term...)}. If {@code core} is not
	 * {@code null}, the literals of the cube are asserted as named terms, and
	 * if the result is {@link SmtResult#UNSAT} the literals in the unsat core
	 * are added to {@code core}.
	 */
	private SmtResult check(
			List<Term> background,
			Cube cube,
			int step,
			Set<SymbolicExpression> core)
			throws SemanticException,
			Stop {
		if (core == null) {
			List<Term> terms = new ArrayList<>(background);
			terms.add(cube.at(unrolling, step, CUBE));
			return check(terms.toArray(Term[]::new));
		}
		Map<String, SymbolicExpression> names = new LinkedHashMap<>();
		Map<String, Term> terms = new LinkedHashMap<>();
		for (SymbolicExpression lit : cube.getLiterals()) {
			// names must be unique: they might survive pops
			String name = "lit!" + (literalNames++);
			names.put(name, lit);
			terms.put(name, unrolling.condition(lit, step, CUBE));
		}
		budget();
		queries++;
		solver.push();
		for (Term t : background)
			solver.assertTerm(t);
		for (Map.Entry<String, Term> t : terms.entrySet())
			solver.assertNamed(t.getKey(), t.getValue());
		SmtResult res = solver.check();
		if (res == SmtResult.UNKNOWN)
			unknown();
		if (res == SmtResult.UNSAT)
			for (String name : solver.getUnsatCore()) {
				SymbolicExpression lit = names.get(name);
				if (lit != null)
					core.add(lit);
			}
		return res;
	}

	private void budget()
			throws Stop {
		if (queries >= maxQueries || System.nanoTime() > deadline)
			throw new Stop(Status.BUDGET_EXHAUSTED);
	}

	/**
	 * Yields the values, in the current model, of the fresh values occurring in
	 * the given expressions that have been encoded under the given key.
	 */
	private Map<FreshValue, String> freshValues(
			Collection<? extends SymbolicExpression> expressions,
			String key) {
		Set<FreshValue> encoded = unrolling.getFreshValues(key);
		List<FreshValue> fresh = new ArrayList<>();
		for (FreshValue v : Projection.freshValues(new ArrayList<>(expressions)))
			if (encoded.contains(v))
				fresh.add(v);
		Map<FreshValue, String> result = new LinkedHashMap<>();
		if (fresh.isEmpty())
			return result;
		List<Term> terms = new ArrayList<>();
		for (FreshValue v : fresh)
			terms.add(unrolling.freshConstant(v, key));
		Map<Term, String> values = solver.getValues(terms);
		for (int i = 0; i < fresh.size(); i++)
			result.put(fresh.get(i), values.get(terms.get(i)));
		return result;
	}

	/**
	 * Replaces the fresh values in the given literals with their values, when
	 * these can be expressed as constants.
	 */
	private Cube project(
			Collection<SymbolicExpression> literals,
			Map<FreshValue, String> values) {
		Map<FreshValue, SymbolicExpression> constants = new LinkedHashMap<>();
		for (Map.Entry<FreshValue, String> v : values.entrySet()) {
			SymbolicExpression c = Projection.toConstant(v.getValue(), types);
			if (c != null)
				constants.put(v.getKey(), c);
		}
		return new Cube(Projection.replace(literals, constants));
	}

	private static Map<InputValue, String> inputs(
			Map<FreshValue, String> values) {
		Map<InputValue, String> result = new LinkedHashMap<>();
		for (Map.Entry<FreshValue, String> v : values.entrySet())
			if (v.getKey() instanceof InputValue)
				result.put((InputValue) v.getKey(), v.getValue());
		return result;
	}

	/**
	 * Yields the values of the identifiers at step {@code 0} in the current
	 * model.
	 */
	private Map<Identifier, String> point() {
		Map<Term, Identifier> terms = new LinkedHashMap<>();
		for (Identifier id : state)
			terms.put(unrolling.var(id, 0), id);
		Map<Identifier, String> result = new LinkedHashMap<>();
		if (!terms.isEmpty())
			for (Map.Entry<Term, String> v : solver.getValues(new ArrayList<>(terms.keySet())).entrySet())
				result.put(terms.get(v.getKey()), v.getValue());
		return result;
	}
}
