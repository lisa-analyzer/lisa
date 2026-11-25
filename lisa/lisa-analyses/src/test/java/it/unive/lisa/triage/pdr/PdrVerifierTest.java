package it.unive.lisa.triage.pdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.triage.bmc.Unrolling;
import it.unive.lisa.triage.model.SummaryValue;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.type.Untyped;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for {@link PdrVerifier}, used as the
 * {@link it.unive.lisa.checks.verification.WarningVerifier} of an analysis of
 * an IMP program with intervals. Tests are skipped if z3 is not available.
 */
public class PdrVerifierTest {

	private static final String PROGRAM = "class test {\n"
			+ "  shallow(x) {\n"
			+ "    def y = 2;\n"
			+ "    if (x is int)\n"
			+ "      if (x == 42)\n"
			+ "        y = 1;\n"
			+ "    return y;\n"
			+ "  }\n"
			+ "  dead() {\n"
			+ "    def z = 0;\n"
			+ "    def w = 0;\n"
			+ "    if (z > 5)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  relational(x) {\n"
			+ "    def y = x;\n"
			+ "    def w = 0;\n"
			+ "    if (x is int)\n"
			+ "      if (x != y)\n"
			+ "        w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  open(x) {\n"
			+ "    def b = this.g(x);\n"
			+ "    def w = 0;\n"
			+ "    if (b is int)\n"
			+ "      if (b == 3)\n"
			+ "        w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  widening(n) {\n"
			+ "    def i = 0;\n"
			+ "    def w = 0;\n"
			+ "    if (n is int) {\n"
			+ "      while (i < n)\n"
			+ "        i = i + 1;\n"
			+ "      if (n >= 0)\n"
			+ "        if (i > n)\n"
			+ "          w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  inc(x) {\n"
			+ "    return x + 1;\n"
			+ "  }\n"
			+ "  summarized(x) {\n"
			+ "    def y = this.inc(x);\n"
			+ "    def w = 0;\n"
			+ "    if (y == x)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  untyped(x, y) {\n"
			+ "    def s = x + y;\n"
			+ "    def w = 0;\n"
			+ "    if (s - x != y)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"

			+ "  integers(x, y) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int && y is int) {\n"
			+ "      def s = x + y;\n"
			+ "      if (s - x != y)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  ilinear(x, y) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int && y is int)\n"
			+ "      if (x + y == 10 && x - y == 4)\n"
			+ "        w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  linear(x, y) {\n"
			+ "    def w = 0;\n"
			+ "    if (x + y == 10 && x - y == 4)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  loop() {\n"
			+ "    def i = 0;\n"
			+ "    def w = 0;\n"
			+ "    while (i < 3)\n"
			+ "      i = i + 1;\n"
			+ "    if (i == 3)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "}";

	private static Program program;

	private static SemanticTool<?, ?> tool;

	@SuppressWarnings("rawtypes")
	private static IndividualWarningVerifier verifier;

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			PdrVerifierTest.tool = tool;
		}

	}

	@BeforeAll
	public static void analyze() throws Exception {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		program = IMPFrontend.processText(PROGRAM);
		for (CFG cfg : program.getAllCFGs())
			if (!program.getEntryPoints().contains(cfg))
				program.addEntryPoint(cfg);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("pdr").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
		verifier = new PdrVerifier<>();
		verifier.init(tool);
	}

	private static CFG cfg(
			String name) {
		return program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
	}

	private static Statement node(
			CFG cfg,
			String text) {
		return cfg.getNodes().stream().filter(n -> n.toString().contains(text)).findFirst()
				.orElseThrow(() -> new AssertionError("No node containing '" + text + "' in " + cfg.getNodes()));
	}

	private static VerificationResult verify(
			String cfg,
			String statement,
			Violation violation)
			throws SemanticException {
		return check(node(cfg(cfg), statement), violation);
	}

	@SuppressWarnings("unchecked")
	private static VerificationResult check(
			Statement st,
			Violation violation)
			throws SemanticException {
		return verifier.verify(tool, new StatementMessage(st, "alarm", null, violation));
	}

	private static BinaryExpression is(
			String variable,
			int value) {
		return new BinaryExpression(BoolType.INSTANCE,
				new Variable(Untyped.INSTANCE, variable, SyntheticLocation.INSTANCE),
				new Constant(Int32Type.INSTANCE, value, SyntheticLocation.INSTANCE),
				ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
	}

	private static String valueOf(
			VerificationResult result,
			String source) {
		for (ReproducingInput input : result.getReproducingInputs())
			if (input.getSource().startsWith(source))
				return input.getValue();
		throw new AssertionError("No input from " + source + " in " + result.getReproducingInputs());
	}

	@Test
	public void shallowBugIsConfirmedWithItsInputs() throws SemanticException {
		Statement ret = node(cfg("shallow"), "return");
		VerificationResult result = check(ret, Violation.condition(ret, is("y", 1)));
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("42", valueOf(result, "parameter x of"));
	}

	@Test
	public void unreachableStatementsAreRefuted() throws SemanticException {
		VerificationResult result = verify("dead", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void relationalFalsePositiveIsRefuted() throws SemanticException {
		// intervals cannot prove that x == y, while pdr learns it
		VerificationResult result = verify("relational", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
		assertTrue(result.getExplanation().contains("inductive invariant"), result.getExplanation());
	}

	@Test
	public void wideningFalsePositiveIsRefuted() throws SemanticException {
		VerificationResult result = verify("widening", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void openCallResultsAreNotInputs() throws SemanticException {
		// the result of an open call is decided by the open call policy, and
		// it is not a user input: the violation is reachable, but it cannot be
		// confirmed
		VerificationResult result = verify("open", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
		assertTrue(result.getExplanation().contains("imprecise return of g"), result.toString());
	}

	@Test
	public void callsToProgramCodeAreNotInputs() throws SemanticException {
		// inc is analyzed modularly: its result is unknown, but it is not an
		// input, so no counterexample through it can be confirmed
		VerificationResult result = verify("summarized", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
		// the explanation tells which values are imprecise
		assertTrue(result.getExplanation().contains("relying on values that are not computed precisely ("),
				result.toString());
		assertTrue(result.getExplanation().contains(SummaryValue.Kind.CALL_RESULT.getDescription()),
				result.toString());
		assertTrue(result.getExplanation().contains("inc"), result.toString());
	}

	@Test
	public void imprecisionsAreExplainedByKind() {
		assertEquals("relying on values that are not computed precisely (call results, heap values): "
				+ "result of call to f; value read from the heap; result of call to g",
				Unrolling.explain(List.of("call results: result of call to f", "heap values: value read from the heap",
						"call results: result of call to g")));
		assertEquals("relying on values that are not computed precisely (unknown)",
				Unrolling.explain(List.of("unknown")));
	}

	@Test
	public void integerArithmeticIsRefuted() throws SemanticException {
		// the type checks make x and y integers, and then s is their sum
		VerificationResult result = verify("integers", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void integerArithmeticIsConfirmed() throws SemanticException {
		// type checks are encoded precisely, so the counterexample is exact
		VerificationResult result = verify("ilinear", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("7", valueOf(result, "parameter x of"));
		assertEquals("3", valueOf(result, "parameter y of"));
	}

	@Test
	public void untypedArithmeticIsNotRefuted() throws SemanticException {
		// x and y might be floating point numbers, where (x + y) - x != y
		// can hold: the violation is possible
		VerificationResult result = verify("untyped", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}

	@Test
	public void untypedArithmeticIsConfirmed() throws SemanticException {
		// x and y are untyped: the first counterexample might use values
		// that are not integers, but a precise one is then searched for
		VerificationResult result = verify("linear", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("7", valueOf(result, "parameter x of"));
		assertEquals("3", valueOf(result, "parameter y of"));
	}

	@Test
	public void loopCounterexample() throws SemanticException {
		VerificationResult result = verify("loop", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
	}

	@SuppressWarnings("unchecked")
	private static VerificationResult check(
			PdrVerifier<?, ?> pdr,
			String cfg,
			String statement,
			Violation violation)
			throws SemanticException {
		PdrVerifier raw = pdr;
		raw.init(tool);
		return raw.verify(tool, new StatementMessage(node(cfg(cfg), statement), "alarm", null, violation));
	}

	/**
	 * Each feature of the verifier can be disabled alone: counterexamples of
	 * real violations are still confirmed (except without case restriction,
	 * where the models are not confirmable), and safe statements are never
	 * confirmed.
	 */
	@Test
	public void eachFeatureCanBeDisabled() throws SemanticException {
		Map<String, Supplier<PdrVerifier<?, ?>>> configurations = new LinkedHashMap<>();
		configurations.put("generalization",
				() -> new PdrVerifier<>().withGeneralization(PdrEngine.Generalization.NONE));
		configurations.put("seeding", () -> new PdrVerifier<>().withSeeding(false));
		configurations.put("decided conditions", () -> new PdrVerifier<>().withDecidedConditions(false));
		configurations.put("invariants", () -> new PdrVerifier<>().withInvariants(false));
		configurations.put("ctgs", () -> new PdrVerifier<>().withCtgs(true));
		configurations.put("recursion summaries", () -> new PdrVerifier<>().withRecursionSummaries(false));
		configurations.put("rounds", () -> new PdrVerifier<>().withMaxRounds(1));
		configurations.put("case growth", () -> new PdrVerifier<>().withCaseGrowth(false));
		configurations.put("chains", () -> new PdrVerifier<>().withMaxChains(0));
		configurations.put("large blocks", () -> new PdrVerifier<>().withLargeBlocks(false));
		configurations.put("liveness", () -> new PdrVerifier<>().withLiveness(false));
		configurations.put("typed fields", () -> new PdrVerifier<>().withTypedFields(false));
		configurations.put("case restriction", () -> new PdrVerifier<>().withCaseRestriction(false));
		configurations.put("strict search", () -> new PdrVerifier<>().withStrictSearch(false));
		configurations.put("float-free search", () -> new PdrVerifier<>().withFloatFreeSearch(false));
		configurations.put("function evaluation", () -> new PdrVerifier<>().withFunctionEvaluation(false));
		configurations.put("fewest literals retry", () -> new PdrVerifier<>().withFewestLiteralsRetry(false));
		configurations.put("fixpoint check", () -> new PdrVerifier<>().withFixpointCheck(false));
		Statement ret = node(cfg("shallow"), "return");
		for (Map.Entry<String, Supplier<PdrVerifier<?, ?>>> conf : configurations.entrySet()) {
			String name = conf.getKey();
			// the safe statements that are not refuted would exhaust the
			// whole budget of the verifier
			Supplier<PdrVerifier<?, ?>> verifier = () -> {
				PdrVerifier<?, ?> v = conf.getValue().get();
				v.setTimeLimit(5_000);
				return v;
			};
			VerificationResult shallow = check(verifier.get(), "shallow", "return",
					Violation.condition(ret, is("y", 1)));
			assertEquals(name.equals("case restriction") ? VerificationOutcome.POSSIBLE : VerificationOutcome.CONFIRMED,
					shallow.getOutcome(), name + ": " + shallow);
			VerificationResult dead = check(verifier.get(), "dead", "w = 1", Violation.reach());
			assertEquals(VerificationOutcome.REFUTED, dead.getOutcome(), name + ": " + dead);
			for (String safe : List.of("relational", "widening", "integers")) {
				VerificationResult result = check(verifier.get(), safe, "w = 1", Violation.reach());
				assertNotEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), name + ", " + safe + ": " + result);
			}
		}
	}

	/**
	 * Every feature of the verifier can be disabled: the verification still
	 * works, with no wrong verdict, although it is less effective.
	 */
	@Test
	public void everyFeatureCanBeDisabled() throws SemanticException {
		PdrVerifier<?, ?> bare = new PdrVerifier<>()
				.withGeneralization(PdrEngine.Generalization.NONE)
				.withSeeding(false)
				.withDecidedConditions(false)
				.withInvariants(false)
				.withCtgs(false)
				.withRecursionSummaries(false)
				.withMaxRounds(1)
				.withCaseGrowth(false)
				.withMaxChains(0)
				.withLargeBlocks(false)
				.withLiveness(false)
				.withTypedFields(false)
				.withCaseRestriction(false)
				.withStrictSearch(false)
				.withFloatFreeSearch(false)
				.withFunctionEvaluation(false)
				.withFewestLiteralsRetry(false);
		// the safe statements that are not refuted would exhaust the whole
		// budget of the verifier
		bare.setTimeLimit(5_000);
		Statement ret = node(cfg("shallow"), "return");
		VerificationResult shallow = check(bare, "shallow", "return", Violation.condition(ret, is("y", 1)));
		// without case restriction, the models are not confirmable
		assertEquals(VerificationOutcome.POSSIBLE, shallow.getOutcome(), shallow.toString());
		shallow = check(bare.withCaseRestriction(true), "shallow", "return", Violation.condition(ret, is("y", 1)));
		assertEquals(VerificationOutcome.CONFIRMED, shallow.getOutcome(), shallow.toString());
		bare.withCaseRestriction(false);
		// the analysis still rules out unreachable statements
		VerificationResult dead = check(bare, "dead", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.REFUTED, dead.getOutcome(), dead.toString());
		// safe statements are never confirmed
		for (String safe : List.of("relational", "widening", "integers")) {
			VerificationResult result = check(bare, safe, "w = 1", Violation.reach());
			assertNotEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), safe + ": " + result);
		}
	}
}
