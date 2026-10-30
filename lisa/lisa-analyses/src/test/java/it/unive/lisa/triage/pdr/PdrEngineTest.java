package it.unive.lisa.triage.pdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.model.AlarmModel;
import it.unive.lisa.triage.model.AlarmModelBuilder;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtSolver;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PdrEngine}, on models of an IMP program analyzed with
 * intervals. Tests are skipped if z3 is not available.
 */
public class PdrEngineTest {

	private static final String PROGRAM = "class test {\n"
			+ "  shallow(x) {\n"
			+ "    def y = 2;\n"
			+ "    if (x == 42)\n"
			+ "      y = 1;\n"
			+ "    return y;\n"
			+ "  }\n"
			+ "  open(x) {\n"
			+ "    def b = this.g(x);\n"
			+ "    def w = 0;\n"
			+ "    if (b == 3)\n"
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
			+ "  bounded(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x > 10)\n"
			+ "      if (x < 5)\n"
			+ "        w = 1;\n"
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
			PdrEngineTest.tool = tool;
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
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static AlarmModel model(
			String name,
			String statement)
			throws SemanticException {
		CFG cfg = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
		Statement st = cfg.getNodes().stream().filter(n -> n.toString().contains(statement)).findFirst().get();
		SemanticTool t = tool;
		AnalyzedCFG result = (AnalyzedCFG) t.getResultOf(cfg).iterator().next();
		return AlarmModelBuilder.build(t, result, st, Violation.reach(), SymbolicStore.DEFAULT_MAX_CASES);
	}

	private static Identifier find(
			AlarmModel model,
			String name) {
		return model.getVocabulary().stream().filter(i -> i.getName().equals(name)).findFirst().get();
	}

	@Test
	public void shallowCounterexample() throws SemanticException {
		AlarmModel model = model("shallow", "y = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.COUNTEREXAMPLE, engine.run(10, 1000));
			List<Obligation> cex = engine.getCounterexample();
			// starts in the initial states, ends at the bad location
			assertEquals(0, cex.get(0).getLevel());
			assertTrue(model.getInitialLocations().contains(cex.get(0).getLocation()));
			assertEquals(model.getBadLocation(), cex.get(cex.size() - 1).getLocation());
			assertNull(cex.get(cex.size() - 1).getParent());
			assertEquals("(Value.Int 42)", cex.get(0).getCube().getLiterals().get(find(model, "x")));
		}
	}

	@Test
	public void openCallResultsAreInputs() throws SemanticException {
		AlarmModel model = model("open", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.COUNTEREXAMPLE, engine.run(10, 1000));
			assertTrue(engine.getCounterexample().stream().flatMap(o -> o.getInputs().values().stream())
					.anyMatch(v -> v.equals("(Value.Int 3)") || v.equals("3")), engine.getCounterexample()
							.toString());
		}
	}

	@Test
	public void loopCounterexample() throws SemanticException {
		AlarmModel model = model("loop", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.COUNTEREXAMPLE, engine.run(20, 10000));
			// the loop is traversed: the counterexample visits the body
			assertTrue(engine.getCounterexample().stream().anyMatch(o -> o.getLocation().toString().contains("+")),
					engine.getCounterexample().toString());
		}
	}

	@Test
	public void infeasibleViolationIsBlocked() throws SemanticException {
		AlarmModel model = model("bounded", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.BLOCKED, engine.run(5, 1000));
			assertNull(engine.getCounterexample());
		}
	}
}
