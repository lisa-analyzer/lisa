package it.unive.lisa.triage.inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.cfg.statement.UserInput;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InputSourceResolverTest {

	/**
	 * {@code nondet} is a native method added to the class after parsing,
	 * returning an unknown integer as library models usually do, while
	 * {@code unknown} is not defined anywhere.
	 */
	private static final String PROGRAM = "class test {\n"
			+ "  main(x) {\n"
			+ "    def n = this.nondet();\n"
			+ "    def u = this.unknown(x);\n"
			+ "    return n + u;\n"
			+ "  }\n"
			+ "}";

	private static CFG main;

	private static NativeCFG nondet;

	private static InputSourceResolver resolver;

	private static final Map<String, Call> calls = new HashMap<>();

	/**
	 * The construct implementing the native {@code nondet} method, a user
	 * input.
	 */
	public static class Nondet
			extends
			UnaryExpression
			implements
			PluggableStatement,
			UserInput {

		private Statement originating;

		public static Nondet build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Nondet(cfg, location, params[0]);
		}

		public Nondet(
				CFG cfg,
				CodeLocation location,
				Expression receiver) {
			super(cfg, location, "nondet", Int32Type.INSTANCE, receiver);
		}

		@Override
		public void setOriginatingStatement(
				Statement st) {
			originating = st;
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			// no additional fields
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				SymbolicExpression expr,
				StatementStore<A> expressions)
				throws SemanticException {
			return interprocedural.getAnalysis().smallStepSemantics(
					state,
					new PushAny(Int32Type.INSTANCE, getLocation()),
					interprocedural.getCurrentContext(),
					this);
		}

	}

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			resolver = new InputSourceResolver(tool);
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
				Nondet.class);
		unit.addInstanceCodeMember(nondet);

		GraphVisitor<CFG, Statement, Edge, Void> visitor = new GraphVisitor<>() {
			@Override
			public boolean visit(
					Void tool,
					CFG graph,
					Statement node) {
				if (node instanceof Call)
					calls.put(((Call) node).getTargetName(), (Call) node);
				return true;
			}
		};
		for (Statement st : main.getNodes())
			st.accept(visitor, null);

		DefaultConfiguration conf = new DefaultConfiguration();
		conf.analysis = DefaultConfiguration.defaultAbstractDomain();
		// calls are resolved only by analyses relying on the call graph
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ContextBasedAnalysis<>();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("input-sources").toString();
		conf.semanticChecks.add(new Capture<>());
		new LiSA(conf).run(program);
		assertNotNull(resolver);
	}

	@Test
	public void parameters() {
		PushAny value = new PushAny(Untyped.INSTANCE, main.getDescriptor().getFormals()[1].getLocation());
		InputSource source = resolver.resolve(value, null);
		assertEquals(InputSource.Kind.PARAMETER, source.getKind());
		assertEquals("parameter x of " + main.getDescriptor().getFullSignatureWithParNames(),
				source.getDescription());

		// values of a different type are only parts of the parameter
		PushAny part = new PushAny(Int32Type.INSTANCE, main.getDescriptor().getFormals()[1].getLocation());
		InputSource partSource = resolver.resolve(part, null);
		assertEquals(InputSource.Kind.PARAMETER, partSource.getKind());
		assertTrue(partSource.getDescription().startsWith("unknown value of type int32 in parameter x of"),
				partSource.getDescription());
	}

	@Test
	public void nativeCalls() {
		Call call = calls.get("nondet");
		InputSource source = resolver.resolve(new PushAny(Int32Type.INSTANCE, call.getLocation()), call);
		assertEquals(InputSource.Kind.CALL_RESULT, source.getKind());
		assertEquals("return of " + nondet.getDescriptor().getFullSignatureWithParNames(), source.getDescription());
	}

	@Test
	public void openCalls() {
		// the result of an open call is decided by the open call policy, and
		// it is not a user input
		Call unknown = calls.get("unknown");
		InputSource source = resolver.resolve(new PushAny(Untyped.INSTANCE, unknown.getLocation()), unknown);
		assertEquals(InputSource.Kind.IMPRECISE_CALL_RESULT, source.getKind());
		assertEquals("return of " + unknown.getFullTargetName(), source.getDescription());
	}

	@Test
	public void otherValuesAndConstraints() {
		SourceCodeLocation loc = new SourceCodeLocation("elsewhere", 1, 1);
		BinaryExpression nonNegative = new BinaryExpression(
				BoolType.INSTANCE,
				new PushAny(Int32Type.INSTANCE, loc),
				new Constant(Int32Type.INSTANCE, 0, loc),
				ComparisonGe.INSTANCE,
				loc);
		InputSource source = resolver.resolve(
				new PushFromConstraints(Int32Type.INSTANCE, loc, Set.of(nonNegative)),
				null);
		assertEquals(InputSource.Kind.UNKNOWN, source.getKind());
		assertEquals("unknown value of type int32", source.getDescription());
		assertEquals(Set.of(nonNegative), source.getConstraints());

		ReproducingInput input = source.toReproducingInput(new UniqueScope<>(), 2, "42");
		assertEquals(loc, input.getLocation());
		assertEquals("unknown value of type int32", input.getSource());
		assertEquals("<unique>", input.getContext());
		assertEquals(2, input.getOccurrence());
		assertEquals("42", input.getValue());
	}
}
