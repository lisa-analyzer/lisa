package it.unive.lisa.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for the verification of warnings after the semantic checks,
 * through a {@link FirstViolationVerifier}. Each method of the analyzed
 * programs contains an assignment {@code w = 1}, on which a warning is issued
 * (with the name of the method as message, so that warnings are verified in
 * alphabetical order) reporting its reachability.
 */
public class FirstViolationVerifierTest {

	private static class WarnOnAssignments<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public boolean visit(
				SemanticTool<A, D> tool,
				CFG graph,
				Statement node) {
			if (node.toString().equals("w = 1"))
				tool.warnOn(node, Violation.reach(), graph.getDescriptor().getName());
			return true;
		}

	}

	private static class WarnOnFirstThrow<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public boolean visit(
				SemanticTool<A, D> tool,
				CFG graph,
				Statement node) {
			if (node.toString().startsWith("throw") && node.getLocation().toString().contains(":3:"))
				tool.warnOn(node, Violation.error(), graph.getDescriptor().getName());
			return true;
		}

	}

	private static Map<String, Message> analyze(
			String program)
			throws Exception {
		return analyze(program, new WarnOnAssignments<>());
	}

	private static Map<String, Message> analyze(
			String program,
			SemanticCheck<?, ?> check)
			throws Exception {
		Program p = IMPFrontend.processText(program);
		for (CFG cfg : p.getAllCFGs())
			if (!p.getEntryPoints().contains(cfg))
				p.addEntryPoint(cfg);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.workdir = Files.createTempDirectory("first-violation").toString();
		conf.semanticChecks.add(check);
		conf.warningVerifier = new FirstViolationVerifier<>();
		LiSAReport report = new LiSA(conf).run(p);
		Map<String, Message> warnings = new HashMap<>();
		for (Message warning : report.getWarnings())
			warnings.put(warning.getMessage(), warning);
		return warnings;
	}

	private static void assertPossible(
			Message warning) {
		assertFalse(warning.isDefinite(), warning.toString());
		assertFalse(warning.isRefuted(), warning.toString());
	}

	@Test
	public void stopsAtTheFirstWarningConfirmedByReachability() throws Exception {
		Map<String, Message> warnings = analyze("class test {\n"
				+ "  a_dead() { def z = 0; def w = 0; if (z > 5) w = 1; return w; }\n"
				+ "  b_always() { def w = 0; w = 1; return w; }\n"
				+ "  c_always() { def w = 0; w = 1; return w; }\n"
				+ "}");
		assertEquals(3, warnings.size(), warnings.toString());
		assertTrue(warnings.get("a_dead").isRefuted(), warnings.toString());
		assertTrue(warnings.get("b_always").isDefinite(), warnings.toString());
		// not verified, since b_always has been confirmed first
		assertPossible(warnings.get("c_always"));
	}

	@Test
	public void movesToPdrIfReachabilityConfirmsNothing() throws Exception {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		Map<String, Message> warnings = analyze("class test {\n"
				+ "  a_dead() { def z = 0; def w = 0; if (z > 5) w = 1; return w; }\n"
				+ "  b_relational(x) { def y = x; def w = 0; if (x != y) w = 1; return w; }\n"
				+ "  c_shallow(x) { def w = 0; if (x == 42) w = 1; return w; }\n"
				+ "  d_shallow(x) { def w = 0; if (x == 7) w = 1; return w; }\n"
				+ "}");
		assertEquals(4, warnings.size(), warnings.toString());
		// refuted by reachability
		assertTrue(warnings.get("a_dead").isRefuted(), warnings.toString());
		// refuted by pdr
		assertTrue(warnings.get("b_relational").isRefuted(), warnings.toString());
		// confirmed by pdr, with the reproducing inputs
		Message confirmed = warnings.get("c_shallow");
		assertTrue(confirmed.isDefinite(), warnings.toString());
		assertTrue(confirmed.getReproducingInputs().stream()
				.anyMatch(i -> i.getSource().startsWith("parameter x of") && i.getValue().equals("42")),
				confirmed.getReproducingInputs().toString());
		// not verified, since c_shallow has been confirmed first
		assertPossible(warnings.get("d_shallow"));
	}

	@Test
	public void errorsReachingAllExitsAreConfirmed() throws Exception {
		// every execution ends with an error, but no single throw is always
		// executed: reachability confirms the warning, with no inputs
		Map<String, Message> warnings = analyze("class A { ~A() {} }\n"
				+ "class test { both(x) { if (x > 0)\n throw new A();\n else\n throw new A(); } }",
				new WarnOnFirstThrow<>());
		assertTrue(warnings.get("both").isDefinite(), warnings.toString());
		assertTrue(warnings.get("both").getReproducingInputs().isEmpty(), warnings.toString());

		// some executions return normally: reachability cannot confirm the
		// warning, while pdr does, with a positive x
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		warnings = analyze("class A { ~A() {} }\n"
				+ "class test { some(x) { if (x > 0)\n throw new A();\n return x; } }",
				new WarnOnFirstThrow<>());
		Message some = warnings.get("some");
		assertTrue(some.isDefinite(), warnings.toString());
		assertTrue(some.getReproducingInputs().stream()
				.anyMatch(i -> i.getSource().startsWith("parameter x of") && Integer.parseInt(i.getValue()) > 0),
				some.getReproducingInputs().toString());
	}

	@Test
	public void noVerificationWithoutVerifier() throws Exception {
		Program p = IMPFrontend.processText("class test { a_dead() { def z = 0; def w = 0; if (z > 5) w = 1; } }");
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.workdir = Files.createTempDirectory("first-violation").toString();
		conf.semanticChecks.add(new WarnOnAssignments<>());
		LiSAReport report = new LiSA(conf).run(p);
		assertEquals(1, report.getWarnings().size());
		assertPossible(report.getWarnings().iterator().next());
	}
}
