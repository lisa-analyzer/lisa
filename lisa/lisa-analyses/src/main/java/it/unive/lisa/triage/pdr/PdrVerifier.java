package it.unive.lisa.triage.pdr;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.checks.verification.WarningVerifier;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.inputs.ReproducingInputs;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.SmtSolverUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link WarningVerifier} based on property-directed reachability
 * ({@link PdrEngine}) over the {@link AlarmModel} of the candidate violation,
 * with frames seeded by the invariants computed by the analysis. The verifier
 * yields:
 * <ul>
 * <li>{@link VerificationOutcome#REFUTED} if the statement is unreachable or
 * the violation cannot happen according to the analysis, or if an inductive
 * invariant excluding the violation is found;</li>
 * <li>{@link VerificationOutcome#CONFIRMED} if a counterexample is found, all
 * of its steps are encoded exactly, and the model starts from the beginning
 * of the program: the reproducing inputs are the values of the parameters in
 * the initial state and the values of the inputs consumed along the
 * counterexample (note that confirmations are not yet certified by
 * execution);</li>
 * <li>{@link VerificationOutcome#POSSIBLE} otherwise (imprecise
 * counterexamples, exhausted budgets, solver failures, or no solver
 * available).</li>
 * </ul>
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of {@link AbstractLattice} produced by the analysis
 * @param <D> the type of {@link AbstractDomain} used by the analysis
 */
public class PdrVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		WarningVerifier<A, D> {

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

	private final int maxLevel;

	private final int maxQueries;

	private final long maxMillis;

	private final Supplier<SmtSolver> solvers;

	private final int maxCases;

	private SemanticTool<A, D> tool;

	private PdrEngine.Generalization generalization = PdrEngine.Generalization.BOTH;

	private boolean seeding = true;

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
	 *                       milliseconds ({@code 0} or negative means no
	 *                       limit)
	 * @param solvers    the supplier of the solvers to use (one per candidate
	 *                       violation)
	 * @param maxCases   the maximum number of cases of each transition of
	 *                       the model
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
		AlarmModel model = AlarmModelBuilder.build(tool, result, st, violation, maxCases);
		if (model.getSeeds(model.getBadLocation()) == null)
			return new VerificationResult(VerificationOutcome.REFUTED, "the statement is unreachable");
		if (model.getBadCases().isEmpty())
			return new VerificationResult(VerificationOutcome.REFUTED, "the violation cannot happen");

		SmtSolver solver;
		try {
			solver = solvers.get();
		} catch (SmtSolverUnavailableException e) {
			return new VerificationResult(VerificationOutcome.POSSIBLE, "no SMT solver available: " + e.getMessage());
		}

		try (SmtSolver s = solver) {
			PdrEngine engine = new PdrEngine(model, s);
			engine.setGeneralization(generalization);
			engine.setSeeding(seeding);
			engine.setAbstraction(new DomainAbstraction<>(tool.getAnalysis(), result));
			PdrEngine.Status status = engine.run(maxLevel, maxQueries, maxMillis);
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

	private static VerificationResult counterexample(
			AlarmModel model,
			List<Obligation> cex) {
		ReproducingInputs inputs = new ReproducingInputs(model.getResult().getId());
		Map<Identifier, String> initial = cex.get(0).getPoint();
		for (Map.Entry<Identifier, InputSource> param : model.getParameters().entrySet()) {
			String value = initial.get(param.getKey());
			if (value != null)
				inputs.add(param.getValue(), value);
		}

		boolean exact = true;
		List<Statement> trace = new ArrayList<>();
		for (Obligation o : cex) {
			exact &= o.isExact();
			// obligations restricting their parent to initial states do not
			// correspond to a step
			if (trace.isEmpty() || trace.get(trace.size() - 1) != o.getLocation())
				trace.add(o.getLocation());
			for (Map.Entry<InputValue, String> input : o.getInputs().entrySet())
				inputs.add(input.getKey().getSource(), input.getValue());
		}

		String path = "violation reached in " + (trace.size() - 1) + " steps through " + trace;
		if (!exact)
			return new VerificationResult(VerificationOutcome.POSSIBLE,
					path + ", relying on values that are not computed precisely");
		if (!model.isConfirmable())
			return new VerificationResult(VerificationOutcome.POSSIBLE,
					path + ", from states that are not the initial ones of the program");
		return new VerificationResult(VerificationOutcome.CONFIRMED,
				path + " (not yet certified by execution)", inputs.get());
	}
}
