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
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
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
			+ "    if (x is int)\n"
			+ "      if (x == 42)\n"
			+ "        y = 1;\n"
			+ "    return y;\n"
			+ "  }\n"
			+ "  open(x) {\n"
			+ "    def b = this.g(x);\n"
			+ "    def w = 0;\n"
			+ "    if (b is int)\n"
			+ "      if (b == 3)\n"
			+ "        w = 1;\n"
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
			+ "  relational(x) {\n"
			+ "    def y = x;\n"
			+ "    def w = 0;\n"
			+ "    if (x is int)\n"
			+ "      if (x != y)\n"
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
			+ "  bigloop() {\n"
			+ "    def i = 0;\n"
			+ "    def w = 0;\n"
			+ "    while (i < 1000)\n"
			+ "      i = i + 1;\n"
			+ "    if (i > 1000)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  bounded(x) {\n"
			+ "    def w = 0;\n"
			+ "    if (x is int)\n"
			+ "      if (x > 10)\n"
			+ "        if (x < 5)\n"
			+ "          w = 1;\n"
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static PdrEngine engine(
			String name,
			String statement,
			SmtSolver solver,
			PdrEngine.Generalization generalization,
			boolean seeding)
			throws SemanticException {
		CFG cfg = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name)).findFirst()
				.get();
		AnalyzedCFG result = (AnalyzedCFG) tool.getResultOf(cfg).iterator().next();
		PdrEngine engine = new PdrEngine(model(name, statement), solver);
		engine.setGeneralization(generalization);
		engine.setSeeding(seeding);
		engine.setAbstraction(new DomainAbstraction(tool.getAnalysis(), result));
		return engine;
	}

	@Test
	public void ablation() throws SemanticException {
		// program, statement, whether the violation is real
		Object[][] cases = {
				{ "shallow", "y = 1", true },
				{ "open", "w = 1", true },
				{ "loop", "w = 1", true },
				{ "relational", "w = 1", false },
				{ "widening", "w = 1", false },
				{ "bounded", "w = 1", false },
				{ "bigloop", "w = 1", false } };
		for (PdrEngine.Generalization g : PdrEngine.Generalization.values())
			for (boolean seeding : new boolean[] { true, false })
				for (Object[] c : cases)
					try (SmtSolver s = SmtLibProcessSolver.z3()) {
						PdrEngine engine = engine((String) c[0], (String) c[1], s, g, seeding);
						// without seeds, some refutations exhaust the budget
						// (e.g., widening has to learn that n is an integer):
						// a time limit keeps the test short
						PdrEngine.Status status = engine.run(20, 2000, 10_000);
						if ((boolean) c[2])
							assertTrue(status != PdrEngine.Status.SAFE && status != PdrEngine.Status.BLOCKED,
									c[0] + " " + g + " " + seeding + ": " + status);
						else
							assertTrue(status != PdrEngine.Status.COUNTEREXAMPLE,
									c[0] + " " + g + " " + seeding + ": " + status);
					}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void domainProposals() throws SemanticException {
		// at the loop guard, intervals know that 0 <= i: assuming i > 1 gives
		// the interval [2, +inf], that becomes the proposal 2 <= i
		CFG cfg = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("loop"))
				.findFirst().get();
		AnalyzedCFG result = (AnalyzedCFG) tool.getResultOf(cfg).iterator().next();
		AlarmModel model = model("loop", "w = 1");
		Identifier i = find(model, "i");
		Statement guard = cfg.getNodes().stream().filter(n -> n.toString().contains("<(i, 3)")).findFirst().get();
		DomainAbstraction abs = new DomainAbstraction(tool.getAnalysis(), result);
		Cube cube = new Cube(List.of(new BinaryExpression(BoolType.INSTANCE, i,
				new Constant(Int32Type.INSTANCE, 1, SyntheticLocation.INSTANCE), ComparisonGt.INSTANCE,
				SyntheticLocation.INSTANCE)));
		Cube proposal = abs.abstractCube(guard, cube);
		assertNotNull(proposal);
		assertTrue(proposal.getLiterals().stream().anyMatch(c -> c instanceof BinaryExpression
				&& ((BinaryExpression) c).getOperator() == ComparisonLe.INSTANCE
				&& ((Constant) ((BinaryExpression) c).getLeft()).getValue().equals(2)
				&& ((BinaryExpression) c).getRight().equals(i)), proposal.toString());

		// a cube that contradicts the invariant has no proposal
		Cube negative = new Cube(List.of(new BinaryExpression(BoolType.INSTANCE, i,
				new Constant(Int32Type.INSTANCE, 0, SyntheticLocation.INSTANCE), ComparisonLt.INSTANCE,
				SyntheticLocation.INSTANCE)));
		assertNull(abs.abstractCube(guard, negative));
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
			assertEquals("(Value.Int 42)", cex.get(0).getPoint().get(find(model, "x")));
		}
	}

	@Test
	public void openCallResultsAreNotInputs() throws SemanticException {
		AlarmModel model = model("open", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.COUNTEREXAMPLE, engine.run(10, 1000));
			// the result of the open call is not an input: the counterexample
			// relies on an imprecise value
			assertTrue(engine.getCounterexample().stream().anyMatch(o -> !o.isExact()),
					engine.getCounterexample().toString());
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
	public void relationalFalsePositiveIsRefuted() throws SemanticException {
		AlarmModel model = model("relational", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.SAFE, engine.run(10, 1000), engine.getFrames().toString());
			assertNotNull(engine.getInvariant());
		}
	}

	@Test
	public void wideningFalsePositiveIsRefuted() throws SemanticException {
		AlarmModel model = model("widening", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.SAFE, engine.run(20, 5000), engine.getFrames().toString());
		}
	}

	@Test
	public void budgetsAreEnforced() throws SemanticException {
		AlarmModel model = model("widening", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			assertEquals(PdrEngine.Status.BUDGET_EXHAUSTED, engine.run(20, 10));
			assertEquals(10, engine.getQueries());
		}
	}

	@Test
	public void infeasibleViolationIsBlocked() throws SemanticException {
		AlarmModel model = model("bounded", "w = 1");
		try (SmtSolver s = SmtLibProcessSolver.z3()) {
			PdrEngine engine = new PdrEngine(model, s);
			PdrEngine.Status status = engine.run(5, 1000);
			assertTrue(status == PdrEngine.Status.SAFE || status == PdrEngine.Status.BLOCKED, status.toString());
			assertNull(engine.getCounterexample());
		}
	}
}
