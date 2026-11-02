package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.type.Untyped;
import java.nio.file.Files;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AlarmModelBuilder}, on the results of an analysis of an IMP
 * program with intervals.
 */
public class AlarmModelBuilderTest {

	private static final String PROGRAM = "class A { ~A() {} }\n"
			+ "class test {\n"
			+ "  loop(n) {\n"
			+ "    def i = 0;\n"
			+ "    while (i < 10)\n"
			+ "      i = i + 1;\n"
			+ "    return i;\n"
			+ "  }\n"
			+ "  main(x) {\n"
			+ "    def y = x + 1;\n"
			+ "    if (y > 10)\n"
			+ "      y = 0;\n"
			+ "    else\n"
			+ "      y = y * 2;\n"
			+ "    return y;\n"
			+ "  }\n"
			+ "  thrower(x) {\n"
			+ "    if (x > 0)\n"
			+ "      throw new A();\n"
			+ "    return x;\n"
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
			AlarmModelBuilderTest.tool = tool;
		}

	}

	@BeforeAll
	public static void analyze() throws Exception {
		program = IMPFrontend.processText(PROGRAM);
		for (CFG cfg : program.getAllCFGs())
			if (!program.getEntryPoints().contains(cfg))
				program.addEntryPoint(cfg);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.workdir = Files.createTempDirectory("models").toString();
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static AlarmModel build(
			CFG cfg,
			Statement st,
			Violation violation)
			throws SemanticException {
		SemanticTool t = tool;
		AnalyzedCFG result = (AnalyzedCFG) t.getResultOf(cfg).iterator().next();
		return AlarmModelBuilder.build(t, result, st, violation, SymbolicStore.DEFAULT_MAX_CASES);
	}

	private static Identifier find(
			AlarmModel model,
			String name) {
		return model.getVocabulary().stream().filter(i -> i.getName().equals(name)).findFirst()
				.orElseThrow(() -> new AssertionError("No identifier " + name + " in " + model.getVocabulary()));
	}

	@Test
	public void loopWithConditionViolation() throws SemanticException {
		CFG loop = cfg("loop");
		Statement ret = node(loop, "return");
		Variable i = new Variable(Untyped.INSTANCE, "i", SyntheticLocation.INSTANCE);
		BinaryExpression tooBig = new BinaryExpression(BoolType.INSTANCE, i,
				new Constant(Int32Type.INSTANCE, 10, SyntheticLocation.INSTANCE), ComparisonGt.INSTANCE,
				SyntheticLocation.INSTANCE);
		AlarmModel model = build(loop, ret, Violation.condition(ret, tooBig));

		assertEquals(ret, model.getBadLocation());
		assertEquals(4, model.getLocations().size());
		assertEquals(Set.of(node(loop, "i = 0")), model.getInitialLocations());
		assertTrue(model.isConfirmable());

		// the guard leads both to the body and to the return
		Statement guard = node(loop, "<(i, 10)");
		assertEquals(2, model.getTransitions(guard).size());
		// the return has no successors in the model
		assertTrue(model.getTransitions(ret).isEmpty());

		// the violation is evaluated over the values at the beginning of the
		// return
		assertEquals(1, model.getBadCases().size());
		assertEquals(tooBig, model.getBadCases().get(0).getViolation());

		// intervals give 0 <= i at the loop guard
		Set<SymbolicExpression> seeds = model.getSeeds(guard);
		assertNotNull(seeds);
		assertTrue(seeds.stream().anyMatch(c -> c instanceof BinaryExpression
				&& ((BinaryExpression) c).getOperator() == ComparisonLe.INSTANCE
				&& ((Constant) ((BinaryExpression) c).getLeft()).getValue().equals(0)
				&& ((BinaryExpression) c).getRight().equals(find(model, "i"))), "Missing 0 <= i in " + seeds);

		// i is always an integer
		assertEquals(Sort.INT, model.getSorts().sortOf(find(model, "i")));
	}

	@Test
	public void reachViolationAndParameters() throws SemanticException {
		CFG main = cfg("main");
		Statement ret = node(main, "return");
		AlarmModel model = build(main, ret, Violation.reach());
		assertEquals(main.getNodes().size(), model.getLocations().size());
		assertEquals(1, model.getBadCases().size());
		assertTrue(model.getBadCases().get(0).getPathCondition().isEmpty());

		Identifier x = find(model, "x");
		InputSource source = model.getParameters().get(x);
		assertNotNull(source, "No source for x in " + model.getParameters());
		assertEquals(InputSource.Kind.PARAMETER, source.getKind());
		assertTrue(source.getDescription().startsWith("parameter x of"), source.getDescription());
		assertTrue(model.getParameters().containsKey(find(model, "this")));
	}

	@Test
	public void conditionsDecidedByTheAnalysisAreSeeds() throws SemanticException {
		CFG main = cfg("main");
		AlarmModel model = build(main, node(main, "return"), Violation.reach());
		Statement guard = node(main, ">(y, 10)");
		SymbolicExpression condition = model.getTransitions(guard).stream()
				.filter(e -> e.getEdge() instanceof TrueEdge)
				.findFirst().get().getStore().getCases().iterator().next().getPathCondition().get(0);
		// in the true branch, the analysis knows that the guard holds
		Set<SymbolicExpression> seeds = model.getSeeds(node(main, "y = 0"));
		assertTrue(seeds.contains(condition), condition + " not in " + seeds);
	}

	@Test
	public void errorViolation() throws SemanticException {
		CFG thrower = cfg("thrower");
		Statement thr = node(thrower, "throw");
		AlarmModel model = build(thrower, thr, Violation.error());
		assertEquals(thr, model.getBadLocation());
		assertFalse(model.getBadCases().isEmpty(), "No bad cases");
		// the return cannot reach the throw: it is not part of the model
		assertFalse(model.getLocations().contains(node(thrower, "return")));
		// raising errors of an unrelated type is not a violation
		AlarmModel unrelated = build(thrower, thr, Violation.error(Int32Type.INSTANCE));
		assertTrue(unrelated.getBadCases().isEmpty());
	}
}
