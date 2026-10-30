package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.BadCase;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.FreshValue;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.SymbolicCase;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.TypeSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Property-directed reachability (IC3/PDR) over an {@link AlarmModel}, with
 * frames kept per location ({@link Frames}). Frame {@code F_0} is the set of
 * initial states, while every frame {@code F_i(l)}, with {@code i >= 1}, is
 * the conjunction of the invariant computed by the analysis at {@code l} and
 * of the lemmas of {@code l} with level at least {@code i}.<br>
 * <br>
 * At each level {@code k}, the engine looks for states of {@code F_k} at the
 * bad location that violate the property, and turns each of them into an
 * {@link Obligation}. Obligations are processed lowest level first: an
 * obligation {@code (l, c, i)} is blocked if no case of a transition leads
 * from {@code F_{i-1}} to {@code c} at {@code l} (and, if {@code l} is
 * initial, {@code c} contains no initial state), otherwise the predecessor
 * found becomes a new obligation at level {@code i - 1}. Predecessors are
 * symbolic: they are computed by model-based projection
 * ({@link Projection}) of the cube through the case of the transition taken
 * in the model. An obligation at level {@code 0} contains initial states, so
 * the chain of obligations from it to the bad location is a counterexample.
 * Blocked cubes are generalized by dropping literals, and their negation is
 * added as a lemma.<br>
 * <br>
 * Once every violation at level {@code k} is blocked, lemmas are propagated
 * to the next levels. If a level is left without lemmas of its own, two
 * consecutive frames coincide, and the frame is an inductive invariant: the
 * violation is impossible. This relies on the invariants of the analysis
 * being sound (they hold in every reachable state), while it does not
 * require them to be inductive with respect to the encoding of the
 * transitions.<br>
 * <br>
 * Queries are two-state: the pre-state is step {@code 0} of an
 * {@link Unrolling}, the post-state is step {@code 1}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class PdrEngine {

	/**
	 * The outcome of a run of the engine.
	 */
	public enum Status {
		/**
		 * A chain of obligations from the initial states to the violation
		 * has been found.
		 */
		COUNTEREXAMPLE,

		/**
		 * An inductive invariant excluding the violation has been found.
		 */
		SAFE,

		/**
		 * No violation is possible within the reached level, but no
		 * inductive invariant has been found.
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

	private static final String CUBE = "#cube";

	private final AlarmModel model;

	private final SmtSolver solver;

	private final Unrolling unrolling;

	private final TypeSystem types;

	private final Frames<Statement, Lemma> frames = new Frames<>();

	private final Map<Statement, List<EdgeTransition>> incoming = new HashMap<>();

	private final List<Identifier> state = new ArrayList<>();

	private final PriorityQueue<Obligation> queue = new PriorityQueue<>();

	private long created = 0;

	private int queries = 0;

	private int maxQueries = Integer.MAX_VALUE;

	private List<Obligation> counterexample;

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
		this.model = model;
		this.solver = solver;
		this.unrolling = new Unrolling(model, solver);
		this.types = model.getBadLocation().getProgram().getTypes();
		for (Statement loc : model.getLocations())
			for (EdgeTransition e : model.getTransitions(loc))
				incoming.computeIfAbsent(e.getEdge().getDestination(), d -> new ArrayList<>()).add(e);
		for (Identifier id : model.getVocabulary())
			if (!(id instanceof FreshValue))
				state.add(id);
	}

	/**
	 * Yields the frames.
	 *
	 * @return the frames
	 */
	public Frames<Statement, Lemma> getFrames() {
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
	 * {@link Status#SAFE}: the lemmas of each location, that hold together
	 * with the invariants computed by the analysis.
	 *
	 * @return the invariant, or {@code null}
	 */
	public Map<Statement, Set<Lemma>> getInvariant() {
		return invariantLevel < 0 ? null : frames.invariant(invariantLevel);
	}

	/**
	 * Runs the engine until a counterexample or an inductive invariant is
	 * found, or every violation within {@code maxLevel} steps has been
	 * blocked.
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
		this.maxQueries = maxQueries;
		try {
			// violations in the initial states
			Obligation bad = badObligation(0);
			if (bad != null) {
				counterexample = List.of(bad);
				return Status.COUNTEREXAMPLE;
			}

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
	 * Yields an obligation for a state of {@code F_level} (or of the initial
	 * states, if {@code level} is {@code 0}) at the bad location, violating
	 * the property, or {@code null} if there is none.
	 */
	private Obligation badObligation(
			int level)
			throws SemanticException,
			Stop {
		Statement bad = model.getBadLocation();
		for (BadCase c : model.getBadCases()) {
			List<SymbolicExpression> lits = new ArrayList<>(c.getPathCondition());
			if (c.getViolation() != null)
				lits.add(c.getViolation());
			Cube cube = new Cube(lits);
			Term query = cube.at(unrolling, 0, CUBE);
			String key = "cond@0" + CUBE;
			boolean exact = unrolling.isExact(key);
			if (check(frame(bad, level), query) == SmtResult.SAT) {
				Map<FreshValue, String> values = freshValues(cube.getLiterals(), key);
				Obligation o = new Obligation(bad, project(cube.getLiterals(), values), level, null, null,
						inputs(values), exact, point(), created++);
				solver.pop();
				return o;
			}
			solver.pop();
		}
		return null;
	}

	/**
	 * Processes the queue until it is empty (every obligation is blocked) or
	 * a counterexample is found.
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

			Obligation pred = predecessor(o.getLocation(), o.getCube(), o.getLevel(), o);
			if (pred != null) {
				queue.add(pred);
				queue.add(o);
				continue;
			}

			Cube cube = generalize(o.getLocation(), o.getCube(), o.getLevel());
			frames.add(o.getLocation(), cube.negate(), o.getLevel());
			// the same states might still reach the bad location in more steps
			if (o.getLevel() < frames.depth())
				queue.add(new Obligation(o.getLocation(), o.getCube(), o.getLevel() + 1, o.getParent(),
						o.getEdge(), o.getInputs(), o.isExact(), o.getPoint(), created++));
		}
		return false;
	}

	/**
	 * Yields a predecessor of the given cube at the given location: an
	 * initial state in the cube (as an obligation at level {@code 0}, with no
	 * transition), or the projection of the cube on a state of
	 * {@code F_{level-1}} at another location that reaches the cube in one
	 * transition. Yields {@code null} if there is none (the cube is blocked
	 * at the given level).
	 */
	private Obligation predecessor(
			Statement location,
			Cube cube,
			int level,
			Obligation parent)
			throws SemanticException,
			Stop {
		if (model.getInitialLocations().contains(location)) {
			if (check(frame(location, 0), cube.at(unrolling, 0, CUBE)) == SmtResult.SAT) {
				// no step is taken: the obligation refines its parent
				Obligation init = new Obligation(location, cube, 0, parent, null, Collections.emptyMap(), true,
						point(), created++);
				solver.pop();
				return init;
			}
			solver.pop();
		}
		for (EdgeTransition e : incoming.getOrDefault(location, Collections.emptyList())) {
			Statement src = e.getEdge().getSource();
			if (!model.getLocations().contains(src))
				continue;
			List<SymbolicCase> cases = unrolling.cases(e);
			for (int i = 0; i < cases.size(); i++) {
				Term pre = frame(src, level - 1);
				Term trans = unrolling.transition(0, e, i);
				Term post = cube.at(unrolling, 1, CUBE);
				String caseKey = unrolling.caseKey(0, e, i);
				String cubeKey = "cond@1" + CUBE;
				boolean exact = unrolling.isExact(caseKey) && unrolling.isExact(cubeKey);
				if (check(pre, trans, post) == SmtResult.SAT) {
					SymbolicCase c = cases.get(i);
					List<SymbolicExpression> lits = new ArrayList<>(c.getPathCondition());
					for (SymbolicExpression lit : cube.getLiterals())
						lits.add(c.substitute(lit, e.getStore().getFactory(), null));
					Map<FreshValue, String> values = freshValues(lits, caseKey);
					// fresh values of the cube that survived substitution
					Map<FreshValue, String> fromCube = freshValues(lits, cubeKey);
					fromCube.keySet().removeAll(values.keySet());
					Map<FreshValue, String> all = new LinkedHashMap<>(values);
					all.putAll(fromCube);
					// every input of the case, even if not in the cube, is
					// needed to reproduce the transition
					Map<FreshValue, String> caseInputs = freshValues(unrolling.getFreshValues(caseKey), caseKey);
					Obligation pred = new Obligation(src, project(lits, all), level - 1, parent, e,
							inputs(caseInputs), exact, point(), created++);
					solver.pop();
					return pred;
				}
				solver.pop();
			}
		}
		return null;
	}

	/**
	 * Drops literals from a cube blocked at the given level, as long as the
	 * result is still blocked.
	 */
	private Cube generalize(
			Statement location,
			Cube cube,
			int level)
			throws SemanticException,
			Stop {
		Cube current = cube;
		for (SymbolicExpression lit : cube.getLiterals()) {
			Cube candidate = current.without(lit);
			if (predecessor(location, candidate, level, null) == null)
				current = candidate;
		}
		return current;
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
			for (Statement loc : model.getLocations())
				for (Lemma lemma : frames.exactly(loc, i))
					if (preserved(loc, lemma, i))
						frames.add(loc, lemma, i + 1);
	}

	private boolean preserved(
			Statement location,
			Lemma lemma,
			int level)
			throws SemanticException,
			Stop {
		for (EdgeTransition e : incoming.getOrDefault(location, Collections.emptyList())) {
			Statement src = e.getEdge().getSource();
			if (!model.getLocations().contains(src))
				continue;
			Term pre = frame(src, level);
			Term trans = unrolling.transition(0, e);
			Term post = Term.not(lemma.at(unrolling, 1));
			SmtResult res = check(pre, trans, post);
			solver.pop();
			if (res != SmtResult.UNSAT)
				return false;
		}
		return true;
	}

	/**
	 * Yields frame {@code F_level} at the given location, over step
	 * {@code 0}, including the program counter.
	 */
	private Term frame(
			Statement location,
			int level)
			throws SemanticException {
		if (level == 0)
			return Term.and(unrolling.at(0, location), unrolling.initial());
		List<Term> conj = new ArrayList<>();
		conj.add(unrolling.at(0, location));
		conj.add(unrolling.seeds(location, 0));
		for (Lemma lemma : frames.frame(location, level))
			conj.add(lemma.at(unrolling, 0));
		return Term.and(conj.toArray(Term[]::new));
	}

	/**
	 * Checks the conjunction of the given terms in a new scope, that is left
	 * open (so that the model can be read) and must be closed by the caller.
	 *
	 * @throws Stop if the budget is exhausted or the solver cannot decide
	 *                  (in which case the scope is closed)
	 */
	private SmtResult check(
			Term... terms)
			throws Stop {
		if (queries >= maxQueries)
			throw new Stop(Status.BUDGET_EXHAUSTED);
		queries++;
		solver.push();
		for (Term t : terms)
			solver.assertTerm(t);
		SmtResult res = solver.check();
		if (res == SmtResult.UNKNOWN) {
			solver.pop();
			throw new Stop(Status.UNKNOWN);
		}
		return res;
	}

	/**
	 * Yields the values, in the current model, of the fresh values occurring
	 * in the given expressions that have been encoded under the given key.
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
