package it.unive.lisa.triage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.inputs.InputSource;
import it.unive.lisa.triage.inputs.InputSourceResolver;
import it.unive.lisa.triage.inputs.InputSourceResolverTest;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReplayInterprocedural}: statements containing calls are
 * replayed from the results of an analysis, and the recorded transitions are
 * inspected.
 */
public class ReplayInterproceduralTest {

	private static final String PROGRAM = "class test {\n"
			+ "  main(x) {\n"
			+ "    def a = this.f(x);\n"
			+ "    def b = this.g(x);\n"
			+ "    def c = this.nondet();\n"
			+ "    return a;\n"
			+ "  }\n"
			+ "  f(y) {\n"
			+ "    return y + 1;\n"
			+ "  }\n"
			+ "}";

	private static CFG main;

	private static NativeCFG nondet;

	private static SemanticTool<?, ?> tool;

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			ReplayInterproceduralTest.tool = tool;
		}

	}

	@BeforeAll
	public static void analyze() throws Exception {
		Program program = IMPFrontend.processText(PROGRAM);
		main = program.getAllCFGs().stream().filter(c -> c.getDescriptor().getName().equals("main")).findFirst()
				.get();
		if (!program.getEntryPoints().contains(main))
			program.addEntryPoint(main);

		CompilationUnit unit = (CompilationUnit) main.getDescriptor().getUnit();
		SourceCodeLocation runtime = new SourceCodeLocation("runtime", 0, 0);
		nondet = new NativeCFG(
				new CodeMemberDescriptor(runtime, unit, true, "nondet", Int32Type.INSTANCE,
						new Parameter(runtime, "this", main.getDescriptor().getFormals()[0].getStaticType())),
				InputSourceResolverTest.Nondet.class);
		unit.addInstanceCodeMember(nondet);

		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("replay").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(tool);
	}

	private static Statement node(
			String text) {
		return main.getNodes().stream().filter(n -> n.toString().contains(text)).findFirst()
				.orElseThrow(() -> new AssertionError("No node containing '" + text + "' in " + main.getNodes()));
	}

	@SuppressWarnings("unchecked")
	private static SymbolicCase replay(
			String text)
			throws SemanticException {
		return replay((SemanticTool<DummyA, DummyD>) (SemanticTool<?, ?>) tool, node(text));
	}

	/*
	 * Placeholders to bind the type variables of the tool in a generic
	 * method.
	 */
	private interface DummyA
			extends
			AbstractLattice<DummyA> {
	}

	private interface DummyD
			extends
			AbstractDomain<DummyA> {
	}

	private static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> SymbolicCase replay(
			SemanticTool<A, D> tool,
			Statement st)
			throws SemanticException {
		AnalyzedCFG<A> result = tool.getResultOf(main).iterator().next();
		FreshValueFactory factory = new FreshValueFactory(new InputSourceResolver(tool));
		ReplayDomain<A, D> domain = new ReplayDomain<>(tool.getAnalysis().domain, factory, 8);
		ReplayInterprocedural<A, D> replay = new ReplayInterprocedural<>(tool, result, domain);
		AnalysisState<ReplayState<A>> pre = ReplayStates.lift(result.getAnalysisStateBefore(st), domain);
		AnalysisState<ReplayState<A>> post = st.forwardSemantics(pre, replay, new StatementStore<>(pre).bottom());
		Set<SymbolicCase> cases = post.getExecutionState().getStore().getCases();
		assertEquals(1, cases.size(), "Expected a single case: " + cases);
		return cases.iterator().next();
	}

	private static SymbolicExpression valueOf(
			SymbolicCase c,
			String variable) {
		for (Map.Entry<Identifier, SymbolicExpression> e : c.getUpdates().entrySet())
			if (e.getKey().getName().equals(variable))
				return e.getValue();
		throw new AssertionError("No update for " + variable + " in " + c);
	}

	@Test
	public void callsToCfgsAreSummarized() throws SemanticException {
		SymbolicCase c = replay(" f(");
		SymbolicExpression a = valueOf(c, "a");
		assertTrue(a instanceof SummaryValue, "Not a summary: " + a);
		assertTrue(((SummaryValue) a).getReason().contains("f"), ((SummaryValue) a).getReason());
		assertTrue(c.isHeapHavoc());
	}

	@Test
	public void openCallsProduceInputs() throws SemanticException {
		SymbolicCase c = replay(" g(");
		SymbolicExpression b = valueOf(c, "b");
		assertTrue(b instanceof InputValue, "Not an input: " + b);
		InputSource source = ((InputValue) b).getSource();
		assertEquals(InputSource.Kind.CALL_RESULT, source.getKind());
		assertTrue(source.getDescription().contains("g"), source.getDescription());
		assertTrue(!c.isHeapHavoc());
	}

	@Test
	public void nativeCallsProduceInputsDescribedByTheirSignature() throws SemanticException {
		SymbolicCase c = replay("nondet(");
		SymbolicExpression v = valueOf(c, "c");
		assertTrue(v instanceof InputValue, "Not an input: " + v);
		assertEquals("return of " + nondet.getDescriptor().getFullSignatureWithParNames(),
				((InputValue) v).getSource().getDescription());
	}
}
