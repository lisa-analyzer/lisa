package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
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
import java.util.List;
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

	private static final String CALLS = "class A {\n"
			+ "  f;\n"
			+ "  ~A() {}\n"
			+ "}\n"
			+ "class calls {\n"
			+ "  inc(a) {\n"
			+ "    return a + 1;\n"
			+ "  }\n"
			+ "  rec(n) {\n"
			+ "    if (n <= 0)\n"
			+ "      return 0;\n"
			+ "    def m = n - 1;\n"
			+ "    return this.rec(m) + 1;\n"
			+ "  }\n"
			+ "  caller(x) {\n"
			+ "    def y = this.inc(x);\n"
			+ "    def w = 0;\n"
			+ "    if (y == 5)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  recursive(x) {\n"
			+ "    def y = this.rec(x);\n"
			+ "    def w = 0;\n"
			+ "    if (y == 5)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "  operand(x) {\n"
			+ "    def a = new A();\n"
			+ "    def y = a.f + this.inc(x);\n"
			+ "    def w = 0;\n"
			+ "    if (y == 5)\n"
			+ "      w = 1;\n"
			+ "    return w;\n"
			+ "  }\n"
			+ "}";

	private static Program program;

	private static SemanticTool<?, ?> tool;

	private static Program callsProgram;

	private static SemanticTool<?, ?> callsTool;

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			AlarmModelBuilderTest.tool = tool;
		}

	}

	private static class CaptureCalls<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			AlarmModelBuilderTest.callsTool = tool;
		}

	}

	@BeforeAll
	public static void analyzeCalls() throws Exception {
		callsProgram = IMPFrontend.processText(CALLS);
		for (CFG cfg : callsProgram.getAllCFGs())
			if (!callsProgram.getEntryPoints().contains(cfg))
				callsProgram.addEntryPoint(cfg);
		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("models").toString();
		conf.semanticChecks.add(new CaptureCalls<>());
		new LiSA(conf).run(callsProgram);
		assertNotNull(callsTool);
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

	private static ModelLocation at(
			AlarmModel model,
			Statement node) {
		return new ModelLocation(model.getRootFrame(), node);
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

		assertEquals(at(model, ret), model.getBadLocation());
		assertEquals(4, model.getLocations().size());
		assertEquals(Set.of(at(model, node(loop, "i = 0"))), model.getInitialLocations());
		assertTrue(model.isConfirmable());

		// the guard leads both to the body and to the return
		Statement guard = node(loop, "<(i, 10)");
		assertEquals(2, model.getTransitions(at(model, guard)).size());
		// the return has no successors in the model
		assertTrue(model.getTransitions(at(model, ret)).isEmpty());

		// the violation is evaluated over the values at the beginning of the
		// return
		assertEquals(1, model.getBadCases().size());
		assertEquals(tooBig, model.getBadCases().get(0).getViolation());

		// intervals give 0 <= i at the loop guard
		Set<SymbolicExpression> seeds = model.getSeeds(at(model, guard));
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
	public void largeBlocks() throws SemanticException {
		CFG main = cfg("main");
		Statement ret = node(main, "return");
		AlarmModel model = build(main, ret, Violation.reach());
		AlarmModel blocks = model.withLargeBlocks();
		// only the initial and the bad locations are left, connected by one
		// transition for each branch
		ModelLocation init = at(model, node(main, "y = +(x, 1)"));
		assertEquals(Set.of(init, at(model, ret)), blocks.getLocations());
		assertEquals(model.getInitialLocations(), blocks.getInitialLocations());
		assertEquals(model.getBadLocation(), blocks.getBadLocation());
		List<EdgeTransition> edges = blocks.getTransitions(init);
		assertEquals(2, edges.size(), edges.toString());
		for (EdgeTransition e : edges) {
			assertEquals(at(model, ret), e.getDestination());
			assertEquals(3, e.getComponents().size());
			assertEquals(at(model, node(main, ">(y, 10)")), e.getIntermediateLocations().get(0));
			assertEquals(1, e.getStore().getCases().size());
			SymbolicCase c = e.getStore().getCases().iterator().next();
			// the condition of the branch is expressed over the parameter
			assertTrue(c.getPathCondition().stream().anyMatch(cond -> cond.toString().contains("x")),
					c.toString());
			assertFalse(c.getPathCondition().stream().anyMatch(cond -> cond.toString().contains("y")),
					c.toString());
		}
		// the transitions of the removed locations are still known
		assertNotNull(blocks.getNodeTransitions(at(model, node(main, ">(y, 10)"))));
		// the seeds of the removed locations are not
		assertNull(blocks.getSeeds(at(model, node(main, ">(y, 10)"))));
	}

	@Test
	public void liveness() throws SemanticException {
		CFG main = cfg("main");
		Statement ret = node(main, "return");
		AlarmModel model = build(main, ret, Violation.reach());
		Identifier x = find(model, "x");
		Identifier y = find(model, "y");
		// the guard reads y, computed from x
		assertTrue(model.getLive(at(model, node(main, "y = +(x, 1)"))).contains(x));
		assertTrue(model.getLive(at(model, node(main, ">(y, 10)"))).contains(y));
		assertFalse(model.getLive(at(model, node(main, ">(y, 10)"))).contains(x));
		// reaching the return reads nothing
		assertTrue(model.getLive(at(model, ret)).isEmpty());
		assertTrue(model.getLive(at(model, node(main, "y = 0"))).isEmpty());

		// a condition on y at the return makes it live in the branches
		Variable v = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);
		BinaryExpression big = new BinaryExpression(BoolType.INSTANCE, v,
				new Constant(Int32Type.INSTANCE, 10, SyntheticLocation.INSTANCE), ComparisonGt.INSTANCE,
				SyntheticLocation.INSTANCE);
		model = build(main, ret, Violation.condition(ret, big));
		assertEquals(Set.of(y), model.getLive(at(model, ret)));
		assertTrue(model.getLive(at(model, node(main, "y = *(y, 2)"))).contains(y));
		assertFalse(model.getLive(at(model, node(main, "y = 0"))).contains(y));
	}

	@Test
	public void conditionsDecidedByTheAnalysisAreSeeds() throws SemanticException {
		CFG main = cfg("main");
		AlarmModel model = build(main, node(main, "return"), Violation.reach());
		Statement guard = node(main, ">(y, 10)");
		SymbolicExpression condition = model.getTransitions(at(model, guard)).stream()
				.filter(e -> e.getEdge() instanceof TrueEdge)
				.findFirst().get().getStore().getCases().iterator().next().getPathCondition().get(0);
		// in the true branch, the analysis knows that the guard holds
		Set<SymbolicExpression> seeds = model.getSeeds(at(model, node(main, "y = 0")));
		assertTrue(seeds.contains(condition), condition + " not in " + seeds);
	}

	@Test
	public void errorViolation() throws SemanticException {
		CFG thrower = cfg("thrower");
		Statement thr = node(thrower, "throw");
		AlarmModel model = build(thrower, thr, Violation.error());
		assertEquals(at(model, thr), model.getBadLocation());
		assertFalse(model.getBadCases().isEmpty(), "No bad cases");
		// the return cannot reach the throw: it is not part of the model
		assertFalse(model.getLocations().contains(at(model, node(thrower, "return"))));
		// raising errors of an unrelated type is not a violation
		AlarmModel unrelated = build(thrower, thr, Violation.error(Int32Type.INSTANCE));
		assertTrue(unrelated.getBadCases().isEmpty());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static AlarmModel buildCalls(
			String name,
			Set<Frame> expanded)
			throws SemanticException {
		CFG cfg = callsProgram.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.findFirst().get();
		SemanticTool t = callsTool;
		AnalyzedCFG result = (AnalyzedCFG) t.getResultOf(cfg).iterator().next();
		return AlarmModelBuilder.build(t, result, node(cfg, "w = 1"), Violation.reach(),
				SymbolicStore.DEFAULT_MAX_CASES, expanded);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static AlarmModel buildCalls(
			String name)
			throws SemanticException {
		CFG cfg = callsProgram.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.findFirst().get();
		SemanticTool t = callsTool;
		AnalyzedCFG result = (AnalyzedCFG) t.getResultOf(cfg).iterator().next();
		return AlarmModelBuilder.build(t, result, node(cfg, "w = 1"), Violation.reach(),
				SymbolicStore.DEFAULT_MAX_CASES);
	}

	private static ReplayedCall callAt(
			AlarmModel model,
			ModelLocation loc) {
		List<ReplayedCall> calls = model.getNodeTransitions(loc).getCalls();
		assertEquals(1, calls.size(), calls.toString());
		return calls.get(0);
	}

	@Test
	public void callsAreSummarizedWithoutExpansion() throws SemanticException {
		AlarmModel plain = buildCalls("caller");
		AlarmModel empty = buildCalls("caller", Set.of());
		// no expansion: the models are the same
		assertEquals(plain.dump(), empty.dump());
		assertTrue(empty.getFrames().isEmpty());
		assertFalse(empty.isTruncated());
		for (ModelLocation loc : empty.getLocations()) {
			assertTrue(loc.getFrame().isRoot());
			assertNull(loc.getResumed());
			for (EdgeTransition e : empty.getTransitions(loc))
				assertNotNull(e.getEdge());
		}

		// the call is discovered by the replay, and it can be expanded
		CFG caller = callsProgram.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("caller"))
				.findFirst().get();
		ReplayedCall call = callAt(empty, at(empty, node(caller, "inc(")));
		assertTrue(call.isExpandable(), call.toString());
		assertFalse(call.isExpanded());
		assertEquals(1, call.getCalleeFrames().size());
		assertEquals("inc", call.getCalleeFrames().get(0).getResult().getDescriptor().getName());
	}

	@Test
	public void expandedCallsHaveCalleeFrames() throws SemanticException {
		CFG caller = callsProgram.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("caller"))
				.findFirst().get();
		Statement site = node(caller, "inc(");
		ReplayedCall call = callAt(buildCalls("caller"), new ModelLocation(Frame.root(
				(AnalyzedCFG<?>) callsTool.getResultOf(caller).iterator().next()), site));
		Frame callee = call.getCalleeFrames().get(0);
		AlarmModel model = buildCalls("caller", Set.of(callee));
		assertEquals(Set.of(callee), model.getFrames());
		assertTrue(callAt(model, at(model, site)).isExpanded());

		ModelLocation before = at(model, site);
		ModelLocation resumed = new ModelLocation(model.getRootFrame(), site, call.getCall());
		assertTrue(model.getLocations().contains(resumed), model.getLocations().toString());

		// the location before the call only enters the callee
		List<EdgeTransition> enter = model.getTransitions(before);
		assertEquals(1, enter.size(), enter.toString());
		assertNull(enter.get(0).getEdge());
		assertEquals(callee, enter.get(0).getDestination().getFrame());
		assertTrue(callee.getResult().getEntrypoints().contains(enter.get(0).getDestination().getNode()));

		// the exit of the callee returns where the caller resumes
		ModelLocation exit = new ModelLocation(callee, node(callee.getResult(), "return"));
		assertTrue(model.getLocations().contains(exit), model.getLocations().toString());
		List<EdgeTransition> back = model.getTransitions(exit);
		assertEquals(1, back.size(), back.toString());
		assertNull(back.get(0).getEdge());
		assertEquals(resumed, back.get(0).getDestination());
		SymbolicCase ret = back.get(0).getStore().getCases().iterator().next();
		assertTrue(ret.getUpdates().containsKey(call.getCall().getMetaVariable()), ret.toString());

		// the caller resumes through the edges of the node, with the result
		// of the call
		List<EdgeTransition> next = model.getTransitions(resumed);
		assertEquals(1, next.size(), next.toString());
		assertNotNull(next.get(0).getEdge());
		SymbolicCase resume = next.get(0).getStore().getCases().iterator().next();
		assertEquals(call.getCall().getMetaVariable(), resume.getUpdates().get(find(model, "y")), resume.toString());
		// resume locations have no invariants
		assertTrue(model.getSeeds(resumed).isEmpty());
	}

	@Test
	public void recursiveCallsAreNotExpandable() throws SemanticException {
		AlarmModel model = buildCalls("recursive");
		ReplayedCall outer = model.getLocations().stream()
				.flatMap(l -> model.getNodeTransitions(l).getCalls().stream()).findFirst().get();
		assertTrue(outer.isExpandable(), outer.toString());
		AlarmModel expanded = buildCalls("recursive", Set.copyOf(outer.getCalleeFrames()));
		ReplayedCall inner = expanded.getLocations().stream()
				.filter(l -> !l.getFrame().isRoot())
				.flatMap(l -> expanded.getNodeTransitions(l).getCalls().stream()).findFirst()
				.orElseThrow(() -> new AssertionError(outer + "\n" + expanded.dump()));
		assertFalse(inner.isExpandable(), inner.toString());
		assertEquals("recursive call", inner.getObstacle());
	}

	@Test
	public void callsAfterHeapReadsAreNotExpandable() throws SemanticException {
		AlarmModel model = buildCalls("operand");
		ReplayedCall call = model.getLocations().stream()
				.flatMap(l -> model.getNodeTransitions(l).getCalls().stream())
				.filter(c -> c.getCall().getTargetName().equals("inc")).findFirst().get();
		assertFalse(call.isExpandable(), call.toString());
		assertTrue(call.getObstacle().contains("heap"), call.getObstacle());
	}
}
