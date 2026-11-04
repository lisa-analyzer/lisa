package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.inputs.ReproducingInputs;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.Frame;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.ModelLocation;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.reachability.ReachabilityVerifier;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.SmtSolverUnavailableException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * An {@link IndividualWarningVerifier} based on property-directed reachability
 * ({@link PdrEngine}) over the {@link AlarmModel} of the candidate violation,
 * with frames seeded by the invariants computed by the analysis. The verifier
 * yields:
 * <ul>
 * <li>{@link VerificationOutcome#REFUTED} if the statement is unreachable or
 * the violation cannot happen according to the analysis (first checked through
 * a {@link ReachabilityVerifier}, that queries the abstract states directly,
 * and then through the invariants exported as constraints), or if an inductive
 * invariant excluding the violation is found;</li>
 * <li>{@link VerificationOutcome#CONFIRMED} if a counterexample is found, all
 * of its steps are encoded exactly, and the model starts from the beginning of
 * the program: the reproducing inputs are the values of the parameters in the
 * initial state and the values of the inputs consumed along the counterexample
 * (note that confirmations are not yet certified by execution);</li>
 * <li>{@link VerificationOutcome#POSSIBLE} otherwise (imprecise
 * counterexamples, exhausted budgets, solver failures, or no solver
 * available).</li>
 * </ul>
 * Calls are first summarized. When the counterexample found is imprecise
 * because it relies on the results of summarized calls, or on heap contents
 * they might have written (see
 * {@link Unrolling#dueToCalls(java.util.Collection)}), the expandable calls
 * reached along it are expanded (see {@link AlarmModelBuilder}), and the search
 * is repeated on the new model. This happens for at most a given number of
 * rounds, and until the model reaches its maximum number of locations.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of {@link AbstractLattice} produced by the analysis
 * @param <D> the type of {@link AbstractDomain} used by the analysis
 */
public class PdrVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		IndividualWarningVerifier<A, D> {

	/**
	 * The default maximum level of frames.
	 */
	public static final int DEFAULT_MAX_LEVEL = 50;

	/**
	 * The default maximum number of queries per candidate violation.
	 */
	public static final int DEFAULT_MAX_QUERIES = 10_000;

	/**
	 * The default maximum time per candidate violation, in milliseconds.
	 */
	public static final long DEFAULT_MAX_MILLIS = 60_000;

	/**
	 * The default maximum number of models built per candidate violation, each
	 * expanding more calls than the previous one.
	 */
	public static final int DEFAULT_MAX_ROUNDS = 5;

	private final int maxLevel;

	private final int maxQueries;

	private final long maxMillis;

	private final Supplier<SmtSolver> solvers;

	private final int maxCases;

	private SemanticTool<A, D> tool;

	private PdrEngine.Generalization generalization = PdrEngine.Generalization.BOTH;

	private boolean seeding = true;

	private boolean fixpointCheck = true;

	private Map<String, Integer> statistics = Map.of();

	private boolean inexact;

	private int maxRounds = DEFAULT_MAX_ROUNDS;

	private int maxLocations = AlarmModelBuilder.DEFAULT_MAX_LOCATIONS;

	/**
	 * The trace of the last counterexample found, if it might rely on
	 * summarized calls (see {@link Unrolling#dueToCalls(java.util.Collection)}).
	 */
	private List<ModelLocation> callTrace;

	/**
	 * The number of queries issued while verifying the current warning.
	 */
	private int queries;

	/**
	 * The time by which the verification of the current warning must end.
	 */
	private long deadline;

	private int maxChains = DEFAULT_MAX_CHAINS;

	/**
	 * The default maximum number of chains of calls tried when a violation in
	 * a callee is neither confirmed nor refuted.
	 */
	public static final int DEFAULT_MAX_CHAINS = 3;

	/**
	 * The maximum length of the chains of calls tried when a violation in a
	 * callee is neither confirmed nor refuted.
	 */
	private static final int MAX_CHAIN_LENGTH = 20;

	/**
	 * The maximum number of partial chains explored when looking for chains of
	 * calls.
	 */
	private static final int MAX_EXPLORED_CALLERS = 1000;

	/**
	 * Builds the verifier, with default budgets, using z3 (as configured in
	 * {@link SmtLibProcessSolver}).
	 */
	public PdrVerifier() {
		this(DEFAULT_MAX_LEVEL, DEFAULT_MAX_QUERIES, DEFAULT_MAX_MILLIS, SmtLibProcessSolver::z3,
				SymbolicStore.DEFAULT_MAX_CASES);
	}

	/**
	 * Builds the verifier.
	 *
	 * @param maxLevel   the maximum level of frames
	 * @param maxQueries the maximum number of queries per candidate violation
	 * @param maxMillis  the maximum time per candidate violation, in
	 *                       milliseconds ({@code 0} or negative means no limit)
	 * @param solvers    the supplier of the solvers to use (one per candidate
	 *                       violation)
	 * @param maxCases   the maximum number of cases of each transition of the
	 *                       model
	 */
	public PdrVerifier(
			int maxLevel,
			int maxQueries,
			long maxMillis,
			Supplier<SmtSolver> solvers,
			int maxCases) {
		this.maxLevel = maxLevel;
		this.maxQueries = maxQueries;
		this.maxMillis = maxMillis;
		this.solvers = solvers;
		this.maxCases = maxCases;
	}

	/**
	 * Sets the strategy to generalize blocked cubes (default:
	 * {@link PdrEngine.Generalization#BOTH}, where the domain of the analysis
	 * proposes generalizations through a {@link DomainAbstraction}).
	 *
	 * @param generalization the strategy
	 *
	 * @return this verifier
	 */
	public PdrVerifier<A, D> withGeneralization(
			PdrEngine.Generalization generalization) {
		this.generalization = generalization;
		return this;
	}

	/**
	 * Sets whether or not frames include the invariants computed by the
	 * analysis (default: {@code true}).
	 *
	 * @param seeding whether or not to use the invariants
	 *
	 * @return this verifier
	 */
	public PdrVerifier<A, D> withSeeding(
			boolean seeding) {
		this.seeding = seeding;
		return this;
	}

	/**
	 * Sets the maximum number of chains of calls from the beginning of the
	 * program tried when a violation in a callee (i.e., in a result that is not
	 * the one of the beginning of the program) is neither confirmed nor
	 * refuted (default: {@link #DEFAULT_MAX_CHAINS}; {@code 0}
	 * never tries them). Models starting from the callers can only confirm
	 * violations.
	 *
	 * @param maxChains the maximum number of chains
	 *
	 * @return this verifier
	 */
	public PdrVerifier<A, D> withMaxChains(
			int maxChains) {
		this.maxChains = maxChains;
		return this;
	}

	/**
	 * Sets the maximum number of models built per candidate violation, each
	 * expanding the calls whose results make the previous counterexample
	 * imprecise (default: {@link #DEFAULT_MAX_ROUNDS}; {@code 1} never expands
	 * calls).
	 *
	 * @param maxRounds the maximum number of rounds
	 *
	 * @return this verifier
	 */
	public PdrVerifier<A, D> withMaxRounds(
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
	public PdrVerifier<A, D> withMaxLocations(
			int maxLocations) {
		this.maxLocations = maxLocations;
		return this;
	}

	/**
	 * Yields the statistics of the engine on the last candidate violation that
	 * reached it: the number of queries ({@code queries}), the number of frames
	 * ({@code level}), and the number of generalizations proposed by the domain
	 * ({@code proposals}) and used ({@code accepted}). Queries are counted
	 * over all the models built for the violation, while the other statistics
	 * refer to the last one. Violations decided before running the engine
	 * yield an empty map.
	 *
	 * @return the statistics
	 */
	public Map<String, Integer> getLastStatistics() {
		return statistics;
	}

	/**
	 * Sets whether or not to check the candidate violation against the results
	 * of the analysis through a {@link ReachabilityVerifier} before building
	 * its model (default: {@code true}).
	 *
	 * @param fixpointCheck whether or not to check the results first
	 *
	 * @return this verifier
	 */
	public PdrVerifier<A, D> withFixpointCheck(
			boolean fixpointCheck) {
		this.fixpointCheck = fixpointCheck;
		return this;
	}

	@Override
	public void init(
			SemanticTool<A, D> tool) {
		this.tool = tool;
	}

	@Override
	public VerificationResult verify(
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation)
			throws SemanticException {
		statistics = Map.of();
		if (fixpointCheck) {
			ReachabilityVerifier<A, D> reachability = new ReachabilityVerifier<>();
			reachability.init(tool);
			VerificationResult res = reachability.verify(result, st, violation);
			if (res.getOutcome() == VerificationOutcome.REFUTED)
				return new VerificationResult(VerificationOutcome.REFUTED,
						"refuted by the results of the analysis"
								+ (res.getExplanation() == null ? "" : ": " + res.getExplanation()));
		}
		queries = 0;
		// the time budget is shared by all the models built for the violation
		deadline = System.currentTimeMillis() + maxMillis;
		VerificationResult res = refine(result, st, violation, List.of());
		if (res.getOutcome() != VerificationOutcome.POSSIBLE || isProgramStart(result) || maxChains <= 0)
			return res;

		// the model starts from the states of the result, that are not the
		// initial ones of the program (e.g., the contents of the heap are
		// unknown): models starting from the callers can confirm the violation
		// (but not refute it, since they only consider the executions through
		// specific chains of calls)
		for (List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> chain : chainsTo(result)) {
			if (System.currentTimeMillis() >= deadline)
				break;
			VerificationResult confirmed = refine(result, st, violation, chain);
			if (confirmed.getOutcome() == VerificationOutcome.CONFIRMED)
				return confirmed;
		}
		return res;
	}

	/**
	 * Yields the chains of calls reaching the given result from the beginning
	 * of the program (from the first caller, see
	 * {@link AlarmModelBuilder#build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List)}),
	 * at most {@link #maxChains} of them, shortest first, without recursion.
	 */
	private List<List<Triple<AnalyzedCFG<A>, Statement, CFGCall>>> chainsTo(
			AnalyzedCFG<A> result)
			throws SemanticException {
		ReachabilityVerifier<A, D> reachability = new ReachabilityVerifier<>();
		reachability.init(tool);
		List<List<Triple<AnalyzedCFG<A>, Statement, CFGCall>>> chains = new ArrayList<>();
		LinkedList<List<Triple<AnalyzedCFG<A>, Statement, CFGCall>>> ws = new LinkedList<>();
		ws.add(List.of());
		int explored = 0;
		while (!ws.isEmpty() && chains.size() < maxChains && explored++ < MAX_EXPLORED_CALLERS
				&& System.currentTimeMillis() < deadline) {
			List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> chain = ws.removeFirst();
			AnalyzedCFG<A> callee = chain.isEmpty() ? result : chain.get(0).getLeft();
			for (Triple<AnalyzedCFG<A>, Statement, CFGCall> site : reachability.getReachability()
					.callSitesOf(callee)) {
				AnalyzedCFG<A> caller = site.getLeft();
				if (caller == result || chain.stream().anyMatch(link -> link.getLeft() == caller))
					// recursion
					continue;
				List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> longer = new ArrayList<>();
				longer.add(site);
				longer.addAll(chain);
				if (isProgramStart(caller))
					chains.add(longer);
				else if (longer.size() < MAX_CHAIN_LENGTH)
					ws.add(longer);
			}
		}
		return chains;
	}

	private static boolean isProgramStart(
			AnalyzedCFG<?> result) {
		return result.getId().isStartingId()
				&& result.getProgram().getEntryPoints().stream()
						.anyMatch(cfg -> cfg.getDescriptor().equals(result.getDescriptor()));
	}

	/**
	 * Verifies the violation on models expanding more and more calls, as long
	 * as counterexamples are imprecise because of calls (see
	 * {@link #withMaxRounds(int)}), starting from the given chain of calls (see
	 * {@link AlarmModelBuilder#build(SemanticTool, AnalyzedCFG, Statement, Violation, int, Set, int, List)}).
	 */
	private VerificationResult refine(
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation,
			List<Triple<AnalyzedCFG<A>, Statement, CFGCall>> chain)
			throws SemanticException {
		Set<Frame> expanded = new LinkedHashSet<>();
		for (int round = 1;; round++) {
			AlarmModel model = AlarmModelBuilder.build(tool, result, st, violation, maxCases, expanded,
					maxLocations, chain, deadline);
			if (model.getSeeds(model.getBadLocation()) == null)
				return new VerificationResult(VerificationOutcome.REFUTED, "the statement is unreachable");
			if (model.getBadCases().isEmpty())
				return new VerificationResult(VerificationOutcome.REFUTED, "the violation cannot happen");

			inexact = false;
			callTrace = null;
			statistics = Map.of();
			VerificationResult res = search(model, false);
			List<ModelLocation> trace = callTrace;
			queries += statistics.getOrDefault("queries", 0);
			Map<String, Integer> last = statistics;
			VerificationResult strict = null;
			if (inexact) {
				// look for a counterexample where all values are computed
				// precisely: since the strict encoding under-approximates the
				// executions, only its counterexamples are meaningful
				statistics = Map.of();
				strict = search(model, true);
				queries += statistics.getOrDefault("queries", 0);
			}
			if (!last.isEmpty() || queries > 0) {
				Map<String, Integer> merged = new LinkedHashMap<>(last);
				merged.put("queries", queries);
				statistics = merged;
			}
			if (strict != null && strict.getOutcome() == VerificationOutcome.CONFIRMED)
				return strict;

			if (res.getOutcome() != VerificationOutcome.POSSIBLE || trace == null || round >= maxRounds
					|| model.isTruncated() || System.currentTimeMillis() >= deadline)
				return res;
			Set<Frame> more = model.getExpandableFrames(trace, expanded);
			if (more.isEmpty())
				return res;
			expanded.addAll(more);
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
			PdrEngine engine = new PdrEngine(model, s, strict);
			engine.setGeneralization(generalization);
			engine.setSeeding(seeding);
			engine.setAbstraction(new DomainAbstraction<>(tool.getAnalysis()));
			PdrEngine.Status status = engine.run(maxLevel, maxQueries,
					// at least one millisecond, as 0 means no limit
					Math.max(1, deadline - System.currentTimeMillis()));
			statistics = Map.of("queries", engine.getQueries(), "level", engine.getFrames().depth(), "proposals",
					engine.getProposals(), "accepted", engine.getAcceptedProposals());
			switch (status) {
			case SAFE:
				return new VerificationResult(VerificationOutcome.REFUTED,
						"inductive invariant found: " + engine.getInvariant());
			case COUNTEREXAMPLE:
				return counterexample(model, engine.getCounterexample());
			case BLOCKED:
				return new VerificationResult(VerificationOutcome.POSSIBLE, "no violation within " + maxLevel
						+ " steps, but no inductive invariant found");
			case BUDGET_EXHAUSTED:
				return new VerificationResult(VerificationOutcome.POSSIBLE,
						"budget exhausted after " + engine.getQueries() + " queries, at level "
								+ engine.getFrames().depth());
			case UNKNOWN:
			default:
				return new VerificationResult(VerificationOutcome.POSSIBLE,
						"the solver could not decide a query: " + s.getReasonUnknown());
			}
		} catch (SmtException e) {
			return new VerificationResult(VerificationOutcome.POSSIBLE, "the solver failed: " + e.getMessage());
		}
	}

	private VerificationResult counterexample(
			AlarmModel model,
			List<Obligation> cex) {
		ReproducingInputs inputs = new ReproducingInputs(model.getResult().getId());
		Map<Identifier, String> initial = cex.get(0).getPoint();
		for (Map.Entry<Identifier, InputSource> param : model.getParameters().entrySet()) {
			String value = initial.get(param.getKey());
			if (value != null)
				inputs.add(param.getValue(), value);
		}

		Set<String> imprecisions = new LinkedHashSet<>();
		List<ModelLocation> trace = new ArrayList<>();
		for (Obligation o : cex) {
			imprecisions.addAll(o.getImprecisions());
			// obligations restricting their parent to initial states do not
			// correspond to a step
			if (trace.isEmpty() || !trace.get(trace.size() - 1).equals(o.getLocation()))
				trace.add(o.getLocation());
			// inputs are produced in the context of the frame of the
			// transition producing them
			ScopeId<?> context = (o.getEdge() == null ? o.getLocation() : o.getEdge().getSource()).getFrame()
					.getResult().getId();
			for (Map.Entry<InputValue, String> input : o.getInputs().entrySet())
				inputs.add(input.getKey().getSource(), input.getValue(), context);
		}

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
}
