package it.unive.lisa.triage.bmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
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
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.type.Untyped;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for {@link BoundedModelChecker}, used as the
 * {@link it.unive.lisa.checks.verification.WarningVerifier} of an analysis of
 * an IMP program with intervals. Tests are skipped if z3 is not available.
 */
public class BoundedModelCheckerTest {

	private static final String PROGRAM = "class test {\n"
			+ "  shallow(x) {\n"
			+ "    def y = 2;\n"
			+ "    if (x == 42)\n"
			+ "      y = 1;\n"
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
			+ "    if (x != y)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  open(x) {\n"
			+ "    def b = this.g(x);\n"
			+ "    def w = 0;\n"
			+ "    if (b == 3)\n"
			+ "      w = 1;\n"
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

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			BoundedModelCheckerTest.tool = tool;
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
		conf.warningVerifier = new BoundedModelChecker<>();
		conf.workdir = Files.createTempDirectory("bmc").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
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
		return tool.verify(node(cfg(cfg), statement), violation);
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
		VerificationResult result = tool.verify(ret, Violation.condition(ret, is("y", 1)));
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("42", valueOf(result, "parameter x of"));
	}

	@Test
	public void unreachableStatementsAreRefuted() throws SemanticException {
		VerificationResult result = verify("dead", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void boundedModelCheckingCannotRefute() throws SemanticException {
		// intervals cannot prove that x == y, and no execution reaches the
		// assignment within the bound
		VerificationResult result = verify("relational", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
		assertTrue(result.getExplanation().contains("no violation found"), result.getExplanation());
	}

	@Test
	public void openCallResultsAreInputs() throws SemanticException {
		VerificationResult result = verify("open", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("3", valueOf(result, "return of"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static AlarmModel model(
			CFG cfg,
			Statement st,
			Violation violation)
			throws SemanticException {
		SemanticTool t = tool;
		AnalyzedCFG result = (AnalyzedCFG) t.getResultOf(cfg).iterator().next();
		return AlarmModelBuilder.build(t, result, st, violation, SymbolicStore.DEFAULT_MAX_CASES);
	}

	@Test
	public void modelsCanBeDumped() throws Exception {
		CFG shallow = cfg("shallow");
		Statement ret = node(shallow, "return");
		AlarmModel model = model(shallow, ret, Violation.condition(ret, is("y", 1)));

		String text = model.dump();
		assertTrue(text.contains("(confirmable)"), text);
		assertTrue(text.contains("parameter x of"), text);
		assertTrue(text.contains("[bad]"), text);
		assertTrue(text.contains("bad cases:"), text);

		// the script yields the same answers of the checker when given to z3:
		// the return is reached (with y = 1) after three steps
		String script = BoundedModelChecker.script(model, 3);
		Process z3 = new ProcessBuilder("z3", "-in").redirectErrorStream(true).start();
		try (Writer w = new OutputStreamWriter(z3.getOutputStream(), StandardCharsets.UTF_8)) {
			w.write(script);
		}
		String output = new String(z3.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		z3.waitFor();
		assertEquals(List.of("unsat", "unsat", "unsat", "sat"), List.of(output.trim().split("\\s+")),
				output + "\n" + script);
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
	public void loopsAreUnrolled() throws SemanticException {
		VerificationResult result = verify("loop", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
	}
}
