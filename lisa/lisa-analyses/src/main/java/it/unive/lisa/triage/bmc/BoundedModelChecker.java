package it.unive.lisa.triage.bmc;

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
import it.unive.lisa.triage.inputs.ReproducingInputs;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.EdgeTransition;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.smt.SmtException;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtLibScript;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.SmtSolverUnavailableException;
import it.unive.lisa.triage.smt.Term;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link WarningVerifier} based on bounded model checking: the executions of
 * the {@link AlarmModel} of a violation are unrolled up to a given number of
 * steps (see {@link Unrolling}), and an SMT solver searches for one that
 * reaches the violation. The result is:
 * <ul>
 * <li>{@link VerificationOutcome#REFUTED} if the results of the analysis
 * already exclude the violation (the statement is unreachable, or no bad case
 * exists);</li>
 * <li>{@link VerificationOutcome#CONFIRMED}, with the inputs reproducing the
 * violation, if an execution reaching it is found, the execution only relies
 * on exact transitions, and the model is confirmable (its initial states are
 * the ones of the program);</li>
 * <li>{@link VerificationOutcome#POSSIBLE} otherwise, including when no
 * execution is found within the bound, when the found execution relies on
 * imprecise transitions, and when no solver is available.</li>
 * </ul>
 * Note that confirmed violations are not yet certified by executing the
 * program with the reproducing inputs.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of the abstract states of the analysis
 * @param <D> the type of the domain of the analysis
 */
public class BoundedModelChecker<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		WarningVerifier<A, D> {

	/**
	 * The default number of steps unrolled.
	 */
	public static final int DEFAULT_BOUND = 20;

	private final int bound;

	private final Supplier<SmtSolver> solvers;

	private final int maxCases;

	private SemanticTool<A, D> tool;

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
			Unrolling unrolling = new Unrolling(model, s);
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
	 * invariants of step {@code k + 1}, for each {@code k} up to the bound.
	 * The script can be fed to an SMT solver to inspect the queries offline.
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
		SmtLibScript s = new SmtLibScript();
		Unrolling unrolling = new Unrolling(model, s);
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
		List<Statement> trace = new ArrayList<>();
		for (Term pc : pcs)
			trace.add(unrolling.locationAt(Integer.parseInt(pcValues.get(pc))));

		boolean exact = true;
		ReproducingInputs inputs = new ReproducingInputs(model.getResult().getId());

		Map<Term, Identifier> params = new HashMap<>();
		for (Identifier id : model.getParameters().keySet())
			params.put(unrolling.var(id, 0), id);
		if (!params.isEmpty())
			for (Map.Entry<Term, String> v : solver.getValues(new ArrayList<>(params.keySet())).entrySet())
				inputs.add(model.getParameters().get(params.get(v.getKey())), v.getValue());

		for (int t = 0; t < length; t++)
			for (EdgeTransition edge : model.getTransitions(trace.get(t)))
				if (edge.getEdge().getDestination() == trace.get(t + 1)) {
					String key = unrolling.edgeKey(t, edge);
					exact &= unrolling.isExact(key);
					collectInputs(unrolling, key, solver, inputs);
				}
		String badKey = unrolling.badKey(length);
		exact &= unrolling.isExact(badKey);
		collectInputs(unrolling, badKey, solver, inputs);

		String path = "violation reached in " + length + " steps through " + trace;
		if (!exact)
			return new VerificationResult(VerificationOutcome.POSSIBLE,
					path + ", relying on values that are not computed precisely");
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
			ReproducingInputs inputs) {
		List<InputValue> values = Unrolling.inputsOf(unrolling.getFreshValues(key));
		if (values.isEmpty())
			return;
		List<Term> terms = new ArrayList<>();
		for (InputValue v : values)
			terms.add(unrolling.freshConstant(v, key));
		Map<Term, String> model = solver.getValues(terms);
		for (int i = 0; i < values.size(); i++)
			inputs.add(values.get(i).getSource(), model.get(terms.get(i)));
	}
}
