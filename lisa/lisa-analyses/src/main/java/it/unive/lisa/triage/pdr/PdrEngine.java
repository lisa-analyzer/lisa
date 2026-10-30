package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.FreshValue;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Term;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * The core of property-directed reachability (IC3/PDR) over an
 * {@link AlarmModel}, with frames kept per location ({@link Frames}). Frame
 * {@code F_0} is the set of initial states, while every frame
 * {@code F_i(l)}, with {@code i >= 1}, is the conjunction of the invariant
 * computed by the analysis at {@code l} and of the lemmas of {@code l} with
 * level at least {@code i}.<br>
 * <br>
 * At each level {@code k}, the engine looks for states of {@code F_k} at the
 * bad location that violate the property, and turns each of them into a
 * {@link Obligation}. Obligations are processed lowest level first: an
 * obligation {@code (l, c, i)} is blocked if no transition leads from
 * {@code F_{i-1}} to {@code c} at {@code l} (and, if {@code l} is initial,
 * {@code c} contains no initial state), otherwise each predecessor found
 * becomes a new obligation at level {@code i - 1}. An obligation at level
 * {@code 0} contains initial states, so the chain of obligations from it to
 * the bad location is a counterexample. Blocked cubes are generalized by
 * dropping literals or weakening them to constructor tests, and their
 * negation is added as a lemma.<br>
 * <br>
 * Queries are two-state: the pre-state is step {@code 0} of an
 * {@link Unrolling}, the post-state is step {@code 1}. Lemma propagation and
 * convergence detection are not performed yet.
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
		 * No violation is possible within the reached level.
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

	private final AlarmModel model;

	private final SmtSolver solver;

	private final Unrolling unrolling;

	private final Frames<Statement, Lemma> frames = new Frames<>();

	private final Map<Statement, List<EdgeTransition>> incoming = new HashMap<>();

	private final List<Identifier> state = new ArrayList<>();

	private final PriorityQueue<Obligation> queue = new PriorityQueue<>();

	private long created = 0;

	private int queries = 0;

	private int maxQueries = Integer.MAX_VALUE;

	private List<Obligation> counterexample;

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
	 * Runs the engine until a counterexample is found, or every violation
	 * within {@code maxLevel} steps has been blocked.
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
		// violations in the initial states
		Statement bad = model.getBadLocation();
		Term badTerm = unrolling.bad(0);
		SmtResult res = check(frame(bad, 0), badTerm);
		if (res == SmtResult.UNKNOWN)
			return Status.UNKNOWN;
		if (res == SmtResult.SAT) {
			counterexample = List.of(badObligation(0));
			solver.pop();
			return Status.COUNTEREXAMPLE;
		}
		solver.pop();

		for (int k = 1; k <= maxLevel; k++) {
			if (k > frames.depth())
				frames.extend();
			while (true) {
				res = check(frame(bad, k), badTerm);
				if (res == SmtResult.UNKNOWN)
					return Status.UNKNOWN;
				if (res == SmtResult.UNSAT) {
					solver.pop();
					break;
				}
				queue.add(badObligation(k));
				solver.pop();
				Status st = blockAll();
				if (st != Status.BLOCKED)
					return st;
			}
		}
		return Status.BLOCKED;
	}

	private Obligation badObligation(
			int level) {
		String key = unrolling.badKey(0);
		return new Obligation(model.getBadLocation(), Cube.fromModel(unrolling, 0, state, solver), level, null,
				null, inputs(key), unrolling.isExact(key), created++);
	}

	private Status blockAll()
			throws SemanticException {
		while (!queue.isEmpty()) {
			if (queries >= maxQueries) {
				queue.clear();
				return Status.BUDGET_EXHAUSTED;
			}
			Obligation o = queue.poll();
			if (o.getLevel() == 0) {
				counterexample = new ArrayList<>();
				for (Obligation cur = o; cur != null; cur = cur.getParent())
					counterexample.add(cur);
				queue.clear();
				return Status.COUNTEREXAMPLE;
			}
			// already blocked by a lemma found meanwhile
			SmtResult res = check(frame(o.getLocation(), o.getLevel()), o.getCube().at(unrolling, 0));
			if (res == SmtResult.UNKNOWN)
				return Status.UNKNOWN;
			solver.pop();
			if (res == SmtResult.UNSAT)
				continue;

			Obligation pred = predecessor(o.getLocation(), o.getCube(), o.getLevel(), o);
			if (pred == BLOCKED_UNKNOWN)
				return Status.UNKNOWN;
			if (pred != null) {
				queue.add(pred);
				queue.add(o);
				continue;
			}

			Cube cube = generalize(o.getLocation(), o.getCube(), o.getLevel());
			if (cube == null)
				return Status.UNKNOWN;
			frames.add(o.getLocation(), cube.negate(), o.getLevel());
			// the same states might still reach the bad location in more steps
			if (o.getLevel() < frames.depth())
				queue.add(new Obligation(o.getLocation(), o.getCube(), o.getLevel() + 1, o.getParent(), o.getEdge(),
						o.getInputs(), o.isExact(), created++));
		}
		return Status.BLOCKED;
	}

	private static final Obligation BLOCKED_UNKNOWN = new Obligation(null, null, -1, null, null,
			Collections.emptyMap(), false, -1);

	/**
	 * Yields a predecessor of the given cube at the given location: an
	 * initial state in the cube (as an obligation at level {@code 0}), or a
	 * state of {@code F_{level-1}} at another location that reaches the cube
	 * in one transition. Yields {@code null} if there is none (the cube is
	 * blocked at the given level), and {@link #BLOCKED_UNKNOWN} if the solver
	 * cannot decide.
	 */
	private Obligation predecessor(
			Statement location,
			Cube cube,
			int level,
			Obligation parent)
			throws SemanticException {
		if (model.getInitialLocations().contains(location)) {
			SmtResult res = check(frame(location, 0), cube.at(unrolling, 0));
			if (res == SmtResult.UNKNOWN)
				return BLOCKED_UNKNOWN;
			if (res == SmtResult.SAT) {
				// no step is taken: the obligation refines its parent
				Obligation init = new Obligation(location, Cube.fromModel(unrolling, 0, state, solver), 0, parent,
						null, Collections.emptyMap(), true, created++);
				solver.pop();
				return init;
			}
			solver.pop();
		}
		for (EdgeTransition e : incoming.getOrDefault(location, Collections.emptyList())) {
			Statement src = e.getEdge().getSource();
			if (!model.getLocations().contains(src))
				continue;
			SmtResult res = check(frame(src, level - 1), unrolling.transition(0, e), cube.at(unrolling, 1));
			if (res == SmtResult.UNKNOWN)
				return BLOCKED_UNKNOWN;
			if (res == SmtResult.SAT) {
				String key = unrolling.edgeKey(0, e);
				Obligation pred = new Obligation(src, Cube.fromModel(unrolling, 0, state, solver), level - 1,
						parent, e, inputs(key), unrolling.isExact(key), created++);
				solver.pop();
				return pred;
			}
			solver.pop();
		}
		return null;
	}

	/**
	 * Generalizes a cube blocked at the given level: each literal is dropped
	 * if the result is still blocked, or otherwise weakened to a constructor
	 * test (see {@link Cube#weaken(Identifier)}) if the result is still
	 * blocked. Yields {@code null} if the solver cannot decide.
	 */
	private Cube generalize(
			Statement location,
			Cube cube,
			int level)
			throws SemanticException {
		Cube current = cube;
		for (Identifier id : cube.getIdentifiers())
			for (Cube candidate : new Cube[] { current.without(id), current.weaken(id) }) {
				if (candidate == null)
					continue;
				Obligation pred = predecessor(location, candidate, level, null);
				if (pred == BLOCKED_UNKNOWN)
					return null;
				if (pred == null) {
					current = candidate;
					break;
				}
			}
		return current;
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
	 * open if the result is {@link SmtResult#SAT} or
	 * {@link SmtResult#UNSAT} (so that the model can be read), and closed
	 * otherwise.
	 */
	private SmtResult check(
			Term... terms) {
		queries++;
		solver.push();
		for (Term t : terms)
			solver.assertTerm(t);
		SmtResult res = solver.check();
		if (res == SmtResult.UNKNOWN)
			solver.pop();
		return res;
	}

	private Map<InputValue, String> inputs(
			String key) {
		List<InputValue> values = Unrolling.inputsOf(unrolling.getFreshValues(key));
		Map<InputValue, String> result = new LinkedHashMap<>();
		if (values.isEmpty())
			return result;
		List<Term> terms = new ArrayList<>();
		for (InputValue v : values)
			terms.add(unrolling.freshConstant(v, key));
		Map<Term, String> model = solver.getValues(terms);
		for (int i = 0; i < values.size(); i++)
			result.put(values.get(i), model.get(terms.get(i)));
		return result;
	}
}
