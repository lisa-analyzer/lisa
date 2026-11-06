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
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.bmc.BoundedModelChecker;
import it.unive.lisa.triage.model.SummaryValue;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for the expansion of calls in {@link PdrVerifier} and
 * {@link BoundedModelChecker}, on the results of a context-sensitive analysis
 * of an IMP program with intervals. Counterexamples relying on the results of
 * summarized calls make the verifiers expand the calls along them. Tests are
 * skipped if z3 is not available.
 */
public class CallExpansionTest {

	private static final String PROGRAM = "class A {\n"
			+ "  f;\n"
			+ "  ~A() {}\n"
			+ "}\n"
			+ "class B {\n"
			+ "  f;\n"
			+ "  ~B(v) {\n"
			+ "    this.f = v;\n"
			+ "  }\n"
			+ "}\n"
			+ "class test {\n"
			+ "  inc(a) {\n"
			+ "    return a + 1;\n"
			+ "  }\n"
			+ "  id(a) {\n"
			+ "    return a;\n"
			+ "  }\n"
			+ "  setF(o, v) {\n"
			+ "    o.f = v;\n"
			+ "  }\n"
			+ "  setCell(a, v) {\n"
			+ "    a[0] = v;\n"
			+ "  }\n"
			+ "  rec(n) {\n"
			+ "    if (n <= 0)\n"
			+ "      return 0;\n"
			+ "    def m = n - 1;\n"
			+ "    return this.rec(m) + 1;\n"
			+ "  }\n"
			+ "  result(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def y = this.inc(x);\n"
			+ "      if (y == 5)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  identity(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def y = this.id(x);\n"
			+ "      if (y != x)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  nested(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def y = this.inc(this.inc(x));\n"
			+ "      if (y == 10)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  twice(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def y = this.inc(x) + this.inc(x);\n"
			+ "      if (y == 10)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  heapBug(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def o = new A();\n"
			+ "      this.setF(o, x);\n"
			+ "      if (o.f == 7)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  cellBug(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def arr = new int[3];\n"
			+ "      this.setCell(arr, x);\n"
			+ "      if (arr[0] == 7)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  cellSafe() {\n"
			+ "    def arr = new int[3];\n"
			+ "    this.setCell(arr, 3);\n"
			+ "    def w = 0;\n"
			+ "    if (arr[0] != 3)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  ctorBug(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def b = new B(x);\n"
			+ "      if (b.f == 3)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  ctorSafe(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def b = new B(x);\n"
			+ "      if (b.f != x)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  heapSafe() {\n"
			+ "    def o = new A();\n"
			+ "    this.setF(o, 3);\n"
			+ "    def w = 0;\n"
			+ "    if (o.f != 3)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  recursive(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def y = this.rec(x);\n"
			+ "      if (y == 3)\n"
			+ "        w = 1;\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  operand(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      def a = new A();\n"
			+ "      a.f = 1;\n"
			+ "      def y = a.f + this.inc(x);\n"
			+ "      if (y == 5)\n"
			+ "        w = 1;\n"
			+ "    }\n"
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
			CallExpansionTest.tool = tool;
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
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("expansion").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static VerificationResult verify(
			IndividualWarningVerifier verifier,
			String name)
			throws SemanticException {
		verifier.init(tool);
		CFG cfg = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
		Statement st = cfg.getNodes().stream().filter(n -> n.toString().equals("w = 1")).findFirst()
				.orElseThrow(() -> new AssertionError("No alarm in " + name));
		return verifier.verify(tool, new StatementMessage(st, "alarm", null, Violation.reach()));
	}

	private static VerificationResult pdr(
			String name)
			throws SemanticException {
		return verify(new PdrVerifier<>().withFixpointCheck(false), name);
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
	public void callResultsAreComputedByExpansion() throws SemanticException {
		VerificationResult result = pdr("result");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("4", valueOf(result, "parameter x of"));
		// the counterexample goes through the code of the callee
		assertTrue(result.getExplanation().contains("[result > inc]"), result.toString());
	}

	@Test
	public void callResultsAreNotComputedWithoutExpansion() throws SemanticException {
		VerificationResult result = verify(new PdrVerifier<>().withFixpointCheck(false).withMaxRounds(1),
				"result");
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
		assertTrue(result.getExplanation().contains(SummaryValue.Kind.CALL_RESULT.getDescription()),
				result.toString());
	}

	@Test
	public void boundedModelCheckingExpandsCalls() throws SemanticException {
		VerificationResult result = verify(new BoundedModelChecker<>(), "result");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("4", valueOf(result, "parameter x of"));
	}

	@Test
	public void boundedModelCheckingDoesNotTrustHavockedHeaps() throws SemanticException {
		// without expansion, the heap is unconstrained after the call: the
		// cell read afterwards is not computed precisely (it used to be
		// confirmed with any value of x)
		VerificationResult result = verify(new BoundedModelChecker<>().withMaxRounds(1), "cellBug");
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
		// with expansion, the write of the callee is modeled
		result = verify(new BoundedModelChecker<>(), "cellBug");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("7", valueOf(result, "parameter x of"));
	}

	@Test
	public void refutationsRelyOnCallees() throws SemanticException {
		VerificationResult result = pdr("identity");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void nestedCallsAreExpanded() throws SemanticException {
		VerificationResult result = pdr("nested");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("8", valueOf(result, "parameter x of"));
	}

	@Test
	public void sequentialCallsAreExpanded() throws SemanticException {
		VerificationResult result = pdr("twice");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("4", valueOf(result, "parameter x of"));
	}

	@Test
	public void heapWritesOfCalleesAreModeled() throws SemanticException {
		VerificationResult result = pdr("cellBug");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("7", valueOf(result, "parameter x of"));
	}

	@Test
	public void heapWritesOfCalleesRefute() throws SemanticException {
		VerificationResult result = pdr("cellSafe");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
		result = pdr("heapSafe");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void fieldsWrittenByCalleesAreRead() throws SemanticException {
		// the constructor of A is expanded too, so the heap is never
		// unconstrained
		VerificationResult result = pdr("heapBug");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("7", valueOf(result, "parameter x of"));
		assertTrue(result.getExplanation().contains("[heapBug > setF]"), result.toString());
	}

	@Test
	public void constructorsAreExpanded() throws SemanticException {
		// the constructor is called after the allocation of the object, in
		// the same node
		VerificationResult result = pdr("ctorBug");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals("3", valueOf(result, "parameter x of"));
		assertTrue(result.getExplanation().contains("[ctorBug > B]"), result.toString());
	}

	@Test
	public void constructorsRefute() throws SemanticException {
		VerificationResult result = pdr("ctorSafe");
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void recursiveCallsAreSummarized() throws SemanticException {
		// the outer call to rec can be expanded, but the recursive one cannot:
		// the violation is real, but it cannot be confirmed
		VerificationResult result = verify(new PdrVerifier<>(PdrVerifier.DEFAULT_MAX_LEVEL, 500,
				PdrVerifier.DEFAULT_MAX_MILLIS, SmtLibProcessSolver::z3, SymbolicStore.DEFAULT_MAX_CASES)
						.withFixpointCheck(false),
				"recursive");
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}

	@Test
	public void callsAfterHeapReadsAreSummarized() throws SemanticException {
		// a.f is read before the call, that might change it: the call is not
		// expanded, and the violation cannot be confirmed
		VerificationResult result = pdr("operand");
		assertNotEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}
}
