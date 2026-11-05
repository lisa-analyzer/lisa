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
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for the errors raised inside expanded callees in
 * {@link PdrVerifier}, on the results of a context-sensitive analysis of an
 * IMP program with intervals: errors leaving a callee reach the handlers of
 * its callers, or the bad location of error violations, under the conditions
 * that make the callee raise them. Tests are skipped if z3 is not available.
 */
public class ErrorExpansionTest {

	private static final String PROGRAM = "class E {\n"
			+ "  ~E() {}\n"
			+ "}\n"
			+ "class G {\n"
			+ "  ~G() {}\n"
			+ "}\n"
			+ "class T {\n"
			+ "  ~T(a) {\n"
			+ "    if (a > 0)\n"
			+ "      throw new E();\n"
			+ "  }\n"
			+ "}\n"
			+ "class test {\n"
			+ "  check(a) {\n"
			+ "    if (a > 0)\n"
			+ "      throw new E();\n"
			+ "    return a;\n"
			+ "  }\n"
			+ "  deep(a) {\n"
			+ "    def r = this.check(a);\n"
			+ "    return r;\n"
			+ "  }\n"
			+ "  escapes(x) {\n"
			+ "    if (x is int) {\n"
			+ "      def r = this.check(x);\n"
			+ "    }\n"
			+ "    return 0;\n"
			+ "  }\n"
			+ "  neverPositive(x) {\n"
			+ "    if (x is int) {\n"
			+ "      if (x <= 0) {\n"
			+ "        def r = this.check(x);\n"
			+ "      }\n"
			+ "    }\n"
			+ "    return 0;\n"
			+ "  }\n"
			+ "  caught(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      try {\n"
			+ "        def r = this.deep(x);\n"
			+ "      } catch (E e) {\n"
			+ "        w = 1;\n"
			+ "      }\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  caughtNever(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int) {\n"
			+ "      if (x < 0) {\n"
			+ "        try {\n"
			+ "          def r = this.deep(x);\n"
			+ "        } catch (E e) {\n"
			+ "          w = 1;\n"
			+ "        }\n"
			+ "      }\n"
			+ "    }\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  creates(x) {\n"
			+ "    if (x is int) {\n"
			+ "      def o = new T(x);\n"
			+ "    }\n"
			+ "    return 0;\n"
			+ "  }\n"
			+ "  shared(x) {\n"
			+ "    if (x is int) {\n"
			+ "      if (x < 0) {\n"
			+ "        try {\n"
			+ "          if (x > 0)\n"
			+ "            throw new E();\n"
			+ "        } catch (G g) {\n"
			+ "          def c = 1;\n"
			+ "        } finally {\n"
			+ "          def f = 1;\n"
			+ "        }\n"
			+ "      }\n"
			+ "    }\n"
			+ "    return 0;\n"
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
			ErrorExpansionTest.tool = tool;
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
		conf.workdir = Files.createTempDirectory("errors").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
	}

	private static VerificationResult verify(
			String name,
			String node,
			Violation violation)
			throws SemanticException {
		return verify(new PdrVerifier<>().withFixpointCheck(false), name, node, violation);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static VerificationResult verify(
			PdrVerifier verifier,
			String name,
			String node,
			Violation violation)
			throws SemanticException {
		verifier.init(tool);
		CFG cfg = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
		Statement st = cfg.getNodes().stream().filter(n -> n.toString().startsWith(node)).findFirst()
				.orElseThrow(() -> new AssertionError("No node " + node + " in " + name));
		return verifier.verify(tool, new StatementMessage(st, "alarm", null, violation));
	}

	private static int valueOf(
			VerificationResult result,
			String source) {
		for (ReproducingInput input : result.getReproducingInputs())
			if (input.getSource().startsWith(source))
				return Integer.parseInt(input.getValue());
		throw new AssertionError("No input from " + source + " in " + result.getReproducingInputs());
	}

	@Test
	public void errorsOfCalleesAreConfirmed() throws SemanticException {
		VerificationResult result = verify("escapes", "r = ", Violation.error());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertTrue(valueOf(result, "parameter x of") > 0, result.toString());
		// the counterexample goes through the throw of the callee
		assertTrue(result.getExplanation().contains("[escapes > check] throw"), result.toString());
	}

	@Test
	public void errorsOfCalleesAreRefuted() throws SemanticException {
		VerificationResult result = verify("neverPositive", "r = ", Violation.error());
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void errorsOfOtherTypesDoNotConfirm() throws SemanticException {
		VerificationResult result = verify("escapes", "r = ", Violation.error(Int32Type.INSTANCE));
		assertNotEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
	}

	@Test
	public void errorsOfCalleesReachTheHandlersOfCallers() throws SemanticException {
		VerificationResult result = verify("caught", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertTrue(valueOf(result, "parameter x of") > 0, result.toString());
		assertTrue(result.getExplanation().contains("[caught > deep > check] throw"), result.toString());
	}

	@Test
	public void errorsOfConstructorsAreNotLostWhenSummarized() throws SemanticException {
		// the errors leaving the constructor called by the object creation
		// are attached to the states after the whole node by the analysis
		VerificationResult result = verify(new PdrVerifier<>().withFixpointCheck(false).withMaxRounds(1), "creates",
				"o = ", Violation.error());
		assertNotEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}

	@Test
	public void errorsOfConstructorsAreConfirmed() throws SemanticException {
		VerificationResult result = verify("creates", "o = ", Violation.error());
		assertEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
		assertTrue(valueOf(result, "parameter x of") > 0, result.toString());
	}

	@Test
	public void sharedFinallyBlocksDoNotConfirm() throws SemanticException {
		// the finally block is shared by the normal and the exceptional
		// paths, and the cfg continues from it to both their destinations:
		// the rethrow reached after a normal completion is not an execution
		VerificationResult result = verify("shared", "throw", Violation.error());
		assertNotEquals(VerificationOutcome.CONFIRMED, result.getOutcome(), result.toString());
	}

	@Test
	public void handlersOfCallersAreNotReachedWithoutErrors() throws SemanticException {
		VerificationResult result = verify("caughtNever", "w = 1", Violation.reach());
		assertEquals(VerificationOutcome.REFUTED, result.getOutcome(), result.toString());
	}
}
