package it.unive.lisa.triage.bmc;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.encoding.ExpressionEncoderFactory;
import it.unive.lisa.triage.inputs.ReproducingInputs;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.Frame;
import it.unive.lisa.triage.model.GlobalWrites;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.ModelLocation;
import it.unive.lisa.triage.model.ResultCache;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtLibScript;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.SmtSolverUnavailableException;
import it.unive.lisa.triage.smt.Term;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An {@link IndividualWarningVerifier} based on bounded model checking: the
 * executions of the {@link AlarmModel} of a violation are unrolled up to a
 * given number of steps (see {@link Unrolling}), and an SMT solver searches for
 * one that reaches the violation. The result is:
 * <ul>
 * <li>{@link VerificationOutcome#REFUTED} if the results of the analysis
 * already exclude the violation (the statement is unreachable, or no bad case
 * exists);</li>
 * <li>{@link VerificationOutcome#CONFIRMED}, with the inputs reproducing the
 * violation, if an execution reaching it is found, the execution only relies on
 * exact transitions, and the model is confirmable (its initial states are the
 * ones of the program);</li>
 * <li>{@link VerificationOutcome#POSSIBLE} otherwise, including when no
 * execution is found within the bound, when the found execution relies on
 * imprecise transitions, and when no solver is available.</li>
 * </ul>
 * Note that confirmed violations are not yet certified by executing the program
 * with the reproducing inputs.<br>
 * <br>
 * Calls are first summarized. When the execution found is imprecise because it
 * relies on the results of summarized calls, or on heap contents they might
 * have written (see {@link Unrolling#dueToCalls(java.util.Collection)}), the
 * expandable calls reached along it are expanded (see
 * {@link AlarmModelBuilder}), and the search is repeated on the new model. This
 * happens for at most a given number of rounds, and until the model reaches its
 * maximum number of locations.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of the abstract states of the analysis
 * @param <D> the type of the domain of the analysis
 */
public class BoundedModelChecker<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		IndividualWarningVerifier<A, D> {

	/**
	 * The default number of steps unrolled.
	 */
	public static final int DEFAULT_BOUND = 20;

	/**
	 * The default maximum number of models built per candidate violation, each
	 * expanding more calls than the previous one.
	 */
	public static final int DEFAULT_MAX_ROUNDS = 5;

	private final int bound;

	private final Supplier<SmtSolver> solvers;

	private final int maxCases;

	/**
	 * The information computed from the results of the analysis, shared by all
	 * the models built over them.
	 */
	private ResultCache<A> cache;

	private SemanticTool<A, D> tool;

	private boolean inexact;

	private int maxRounds = DEFAULT_MAX_ROUNDS;

	private boolean largeBlocks = true;

	private int maxLocations = AlarmModelBuilder.DEFAULT_MAX_LOCATIONS;

	private ExpressionEncoderFactory encoders = ExpressionEncoderFactory.DEFAULT;

	private GlobalWrites globalWrites = GlobalWrites.UNKNOWN;

	/**
	 * The trace of the last execution found, if it might rely on summarized
	 * calls (see {@link Unrolling#dueToCalls(java.util.Collection)}).
	 */
	private List<ModelLocation> callTrace;

	/**
	 * Builds the verifier, unrolling {@link #DEFAULT_BOUND} steps with z3 (see
	 * {@link SmtLibProcessSolver#z3()}).
	 */
	public BoundedModelChecker() {
		this(DEFAULT_BOUND, SmtLibProcessSolver::z3, SymbolicStore.DEFAULT_MAX_CASES);
	}

	/**
	 * Builds the verifier.
	 *
	 * @param bound    the number of steps to unroll
	 * @param solvers  the supplier of the solvers to use (one for each
	 *                     verification)
	 * @param maxCases the maximum number of cases of each transition of the
	 *                     models
	 */
	public BoundedModelChecker(
			int bound,
			Supplier<SmtSolver> solvers,
			int maxCases) {
		this.bound = bound;
		this.solvers = solvers;
		this.maxCases = maxCases;
	}

	/**
	 * Sets whether or not sequences of transitions of the models are composed
	 * into single ones before searching them (see
	 * {@link AlarmModel#withLargeBlocks()}; default: {@code true}). With
	 * composed transitions, the bound limits the number of composed
	 * transitions.
	 *
	 * @param largeBlocks whether or not to compose transitions
	 *
	 * @return this verifier
	 */
	public BoundedModelChecker<A, D> withLargeBlocks(
			boolean largeBlocks) {
		this.largeBlocks = largeBlocks;
		return this;
	}

	/**
	 * Sets the maximum number of models built per candidate violation, each
	 * expanding the calls whose results make the previous execution imprecise
	 * (default: {@link #DEFAULT_MAX_ROUNDS}; {@code 1} never expands calls).
	 *
	 * @param maxRounds the maximum number of rounds
	 *
	 * @return this verifier
	 */
	public BoundedModelChecker<A, D> withMaxRounds(
			int maxRounds) {
		this.maxRounds = maxRounds;
		return this;
	}

	/**
	 * Sets the maximum number of locations of a model, beyond which calls are
	 * no longer expanded (default:
	 * {@link AlarmModelBuilder#DEFAULT_MAX_LOCATIONS}).
	 *
	 * @param maxLocations the maximum number of locations
	 *
	 * @return this verifier
	 */
	public BoundedModelChecker<A, D> withMaxLocations(
			int maxLocations) {
		this.maxLocations = maxLocations;
		return this;
	}

	/**
	 * Sets the global variables written by the code of each member of the
	 * program, as the frontend knows them (default:
	 * {@link GlobalWrites#UNKNOWN}): the calls that the models summarize then
	 * leave unchanged the global variables that their callees cannot write,
	 * instead of making all of them unknown.
	 *
	 * @param globalWrites the global writes
	 *
	 * @return this verifier
	 */
	public BoundedModelChecker<A, D> withGlobalWrites(
			GlobalWrites globalWrites) {
		this.globalWrites = globalWrites;
		return this;
	}

	/**
	 * Sets the factory of the encoders of expressions (default:
	 * {@link ExpressionEncoderFactory#DEFAULT}, knowing only the operators of
	 * LiSA). Frontends can pass encoders knowing their own operators (see
	 * {@link it.unive.lisa.triage.encoding.ExpressionEncoder}), so that
	 * executions relying on them can be confirmed.
	 *
	 * @param encoders the factory of the encoders
	 *
	 * @return this verifier
	 */
	public BoundedModelChecker<A, D> withEncoder(
			ExpressionEncoderFactory encoders) {
		this.encoders = encoders;
		return this;
	}

	@Override
	public void init(
			SemanticTool<A, D> tool) {
		this.tool = tool;
		this.cache = new ResultCache<>();
		this.cache.setGlobalWrites(globalWrites);
	}

	@Override
	public VerificationResult verify(
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation)
			throws SemanticException {
		Set<Frame> expanded = new LinkedHashSet<>();
		// the transitions of the locations that are not affected by the calls
		// expanded by each round are extracted once
		AlarmModelBuilder.Reuse reuse = new AlarmModelBuilder.Reuse();
		for (int round = 1;; round++) {
			AlarmModel model = AlarmModelBuilder.build(tool, result, st, violation, maxCases, expanded,
					maxLocations, List.of(), Long.MAX_VALUE, cache, reuse);
			if (model.getSeeds(model.getBadLocation()) == null)
				return new VerificationResult(VerificationOutcome.REFUTED, "the statement is unreachable");
			if (model.getBadCases().isEmpty())
				return new VerificationResult(VerificationOutcome.REFUTED, "the violation cannot happen");

			inexact = false;
			callTrace = null;
			// the calls to expand are found on the original model, through
			// the intermediate locations of the traces
			AlarmModel searched = largeBlocks ? model.withLargeBlocks() : model;
			VerificationResult res = search(searched, false);
			List<ModelLocation> trace = callTrace;
			if (inexact) {
				// look for a counterexample where all values are computed
				// precisely
				VerificationResult strict = search(searched, true);
				if (strict.getOutcome() == VerificationOutcome.CONFIRMED)
					return strict;
			}

			if (res.getOutcome() != VerificationOutcome.POSSIBLE || trace == null || round >= maxRounds
					|| model.isTruncated())
				return res;
			Set<Frame> more = model.getExpandableFrames(trace, expanded);
			if (more.isEmpty())
				return res;
			expanded.addAll(more);
			reuse.expand(more);
		}
	}

	private VerificationResult search(
			AlarmModel model,
			boolean strict)
			throws SemanticException {
		SmtSolver solver;
		try {
			solver = solvers.get();
		} catch (SmtSolverUnavailableException e) {
			return new VerificationResult(VerificationOutcome.POSSIBLE, "no SMT solver available: " + e.getMessage());
		}

		try (SmtSolver s = solver) {
			Unrolling unrolling = new Unrolling(model, s, strict, encoders);
			s.assertTerm(unrolling.initial());
			s.assertTerm(unrolling.invariants(0));
			for (int k = 0; k <= bound; k++) {
				Term bad = unrolling.bad(k);
				s.push();
				s.assertTerm(bad);
				SmtResult res = s.check();
				if (res == SmtResult.SAT) {
					VerificationResult cex = counterexample(unrolling, k, s);
					s.pop();
					return cex;
				}
				if (res == SmtResult.UNKNOWN)
					return new VerificationResult(VerificationOutcome.POSSIBLE,
							"the solver could not decide whether the violation happens in " + k + " steps: "
									+ s.getReasonUnknown());
				s.pop();
				if (k < bound) {
					s.assertTerm(unrolling.transition(k));
					s.assertTerm(unrolling.invariants(k + 1));
				}
			}
			return new VerificationResult(VerificationOutcome.POSSIBLE,
					"no violation found within " + bound + " steps");
		} catch (SmtException e) {
			return new VerificationResult(VerificationOutcome.POSSIBLE, "the solver failed: " + e.getMessage());
		}
	}

	/**
	 * Yields the SMT-LIB 2 script containing the queries that bounded model
	 * checking issues on the given model, up to the given bound: after the
	 * declarations, the initial states, and the invariants of the first step,
	 * the script checks (in a pushed scope) whether the bad location can be
	 * reached in {@code k} steps, and then adds the transitions and the
	 * invariants of step {@code k + 1}, for each {@code k} up to the bound. The
	 * script can be fed to an SMT solver to inspect the queries offline.
	 *
	 * @param model the model
	 * @param bound the maximum number of steps
	 *
	 * @return the script
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public static String script(
			AlarmModel model,
			int bound)
			throws SemanticException {
		return script(model, bound, ExpressionEncoderFactory.DEFAULT);
	}

	/**
	 * Yields the SMT-LIB 2 script containing the queries that bounded model
	 * checking issues on the given model, up to the given bound, as
	 * {@link #script(AlarmModel, int)}, encoding expressions with the encoders
	 * built by the given factory.
	 *
	 * @param model    the model
	 * @param bound    the maximum number of steps
	 * @param encoders the factory of the encoders of expressions
	 *
	 * @return the script
	 *
	 * @throws SemanticException if an expression cannot be encoded
	 */
	public static String script(
			AlarmModel model,
			int bound,
			ExpressionEncoderFactory encoders)
			throws SemanticException {
		SmtLibScript s = new SmtLibScript();
		Unrolling unrolling = new Unrolling(model, s, false, encoders);
		s.assertTerm(unrolling.initial());
		s.assertTerm(unrolling.invariants(0));
		for (int k = 0; k <= bound; k++) {
			Term bad = unrolling.bad(k);
			s.push();
			s.assertTerm(bad);
			s.check();
			s.pop();
			if (k < bound) {
				s.assertTerm(unrolling.transition(k));
				s.assertTerm(unrolling.invariants(k + 1));
			}
		}
		return s.getScript();
	}

	private VerificationResult counterexample(
			Unrolling unrolling,
			int length,
			SmtSolver solver) {
		AlarmModel model = unrolling.getModel();

		List<Term> pcs = new ArrayList<>();
		for (int t = 0; t <= length; t++)
			pcs.add(unrolling.pc(t));
		Map<Term, String> pcValues = solver.getValues(pcs);
		List<ModelLocation> trace = new ArrayList<>();
		for (Term pc : pcs)
			trace.add(unrolling.locationAt(Integer.parseInt(pcValues.get(pc))));

		Set<String> imprecisions = new LinkedHashSet<>();
		ReproducingInputs inputs = new ReproducingInputs(model.getResult().getId());

		Map<Term, Identifier> params = new LinkedHashMap<>();
		for (Identifier id : model.getParameters().keySet())
			params.put(unrolling.var(id, 0), id);
		if (!params.isEmpty())
			for (Map.Entry<Term, String> v : solver.getValues(new ArrayList<>(params.keySet())).entrySet())
				inputs.add(model.getParameters().get(params.get(v.getKey())), v.getValue());

		// the trace, including the locations composed transitions pass
		// through
		List<ModelLocation> full = new ArrayList<>();
		for (int t = 0; t < length; t++) {
			full.add(trace.get(t));
			// the transition taken between the two locations (several might
			// connect them, e.g., composed transitions through different
			// branches)
			EdgeTransition taken = null;
			for (EdgeTransition edge : model.getTransitions(trace.get(t)))
				if (edge.getDestination().equals(trace.get(t + 1))) {
					if (unrolling.holds(unrolling.edgeKey(t, edge), solver)) {
						taken = edge;
						break;
					}
					if (taken == null)
						taken = edge;
				}
			if (taken == null)
				continue;
			String key = unrolling.edgeKey(t, taken);
			imprecisions.addAll(unrolling.getImprecisions(key, solver));
			collectInputs(unrolling, key, solver, inputs, taken);
			full.addAll(taken.getIntermediateLocations());
		}
		full.add(trace.get(length));
		trace = full;
		String badKey = unrolling.badKey(length);
		imprecisions.addAll(unrolling.getImprecisions(badKey, solver));
		collectInputs(unrolling, badKey, solver, inputs, null, trace.get(trace.size() - 1));

		String path = "violation reached in " + (trace.size() - 1) + " steps through " + trace;
		if (!imprecisions.isEmpty()) {
			if (!inexact && Unrolling.dueToCalls(imprecisions))
				callTrace = trace;
			inexact = true;
			return new VerificationResult(VerificationOutcome.POSSIBLE, path + ", " + Unrolling.explain(imprecisions));
		}
		if (!model.isConfirmable())
			return new VerificationResult(VerificationOutcome.POSSIBLE,
					path + ", from states that are not the initial ones of the program");
		return new VerificationResult(VerificationOutcome.CONFIRMED,
				path + " (not yet certified by execution)", inputs.get());
	}

	private static void collectInputs(
			Unrolling unrolling,
			String key,
			SmtSolver solver,
			ReproducingInputs inputs,
			EdgeTransition edge) {
		collectInputs(unrolling, key, solver, inputs, edge, edge.getSource());
	}

	/**
	 * Collects the inputs of the encoding with the given key, produced by the
	 * given transition (if any) or at the given location: inputs are produced
	 * in the order of the transitions producing them (for composed
	 * transitions), in the context of the frame of the transition producing
	 * them.
	 */
	private static void collectInputs(
			Unrolling unrolling,
			String key,
			SmtSolver solver,
			ReproducingInputs inputs,
			EdgeTransition edge,
			ModelLocation location) {
		List<InputValue> values = new ArrayList<>(Unrolling.inputsOf(unrolling.getFreshValues(key)));
		if (values.isEmpty())
			return;
		if (edge != null)
			// in the order of the transitions producing them, and of their
			// evaluation within a transition
			values.sort(Comparator.comparingInt((
					InputValue v) -> edge.positionOf(v))
					.thenComparingLong(InputValue::getIndex));
		List<Term> terms = new ArrayList<>();
		for (InputValue v : values)
			terms.add(unrolling.freshConstant(v, key));
		Map<Term, String> model = solver.getValues(terms);
		for (int i = 0; i < values.size(); i++) {
			ModelLocation producer = edge == null ? location : edge.componentOf(values.get(i)).getSource();
			inputs.add(values.get(i).getSource(), model.get(terms.get(i)),
					producer.getFrame().getResult().getId());
		}
	}
}
