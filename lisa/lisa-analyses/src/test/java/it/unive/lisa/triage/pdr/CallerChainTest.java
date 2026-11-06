package it.unive.lisa.triage.pdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for {@link PdrVerifier} on warnings inside callees, whose
 * results are not the ones of the beginning of the program: counterexamples
 * starting from the states of the callees are confirmed through models starting
 * from the callers, while refutations of those models are not used. The program
 * is analyzed by a context-sensitive analysis with intervals, starting from
 * {@code main} only. Tests are skipped if z3 is not available.
 */
public class CallerChainTest {

	private static final String PROGRAM = "class test {\n"
			+ "  main(x) {\n"
			+ "    if (x is int) {\n"
			+ "      def r = this.helper(x);\n"
			+ "      def y = x + 1;\n"
			+ "      def s = this.pair(x, y);\n"
			+ "    }\n"
			+ "    return 0;\n"
			+ "  }\n"
			+ "  helper(a) {\n"
			+ "    def w = 0;\n"
			+ "    if (a == 42)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  pair(a, b) {\n"
			+ "    def w = 0;\n"
			+ "    if (a == b)\n"
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
			CallerChainTest.tool = tool;
		}

	}

	@BeforeAll
	public static void analyze() throws Exception {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		program = IMPFrontend.processText(PROGRAM, true);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("chains").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static VerificationResult verify(
			PdrVerifier verifier,
			String name)
			throws SemanticException {
		verifier.init(tool);
		CFG cfg = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
		Statement st = cfg.getNodes().stream().filter(n -> n.toString().equals("w = 1")).findFirst()
				.orElseThrow(() -> new AssertionError("No alarm in " + name));
		return verifier.verify(tool, new StatementMessage(st, "alarm", null, Violation.reach()));
	}

	@Test
	public void counterexamplesInCalleesAreConfirmedFromCallers() throws SemanticException {
		VerificationResult result = verify(new PdrVerifier<>().withFixpointCheck(false), "helper");
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		boolean found = false;
		for (ReproducingInput input : result.getReproducingInputs())
			if (input.getSource().startsWith("parameter x of")) {
				assertEquals("42", input.getValue(), result.toString());
				found = true;
			}
		assertTrue(found, result.toString());
		assertTrue(result.getExplanation().contains("[main > helper]"), result.toString());
		// the value returned by the call is produced in the context of main
		for (ReproducingInput input : result.getReproducingInputs())
			assertEquals("<empty>", input.getContext(), result.toString());
	}

	@Test
	public void counterexamplesInCalleesAreNotConfirmedWithoutCallers() throws SemanticException {
		VerificationResult result = verify(new PdrVerifier<>().withFixpointCheck(false).withMaxChains(0), "helper");
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}

	@Test
	public void refutationsFromCallersAreNotUsed() throws SemanticException {
		// the callers never pass equal values, but the model starting from
		// main only considers the executions through one chain of calls
		VerificationResult result = verify(new PdrVerifier<>().withFixpointCheck(false), "pair");
		assertEquals(VerificationOutcome.POSSIBLE, result.getOutcome(), result.toString());
	}
}
