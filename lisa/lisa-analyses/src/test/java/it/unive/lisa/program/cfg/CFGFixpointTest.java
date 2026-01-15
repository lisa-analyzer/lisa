package it.unive.lisa.program.cfg;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.interprocedural.WorstCasePolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.numeric.IntInterval;
import org.junit.BeforeClass;
import org.junit.Test;

public class CFGFixpointTest {

	private static FixpointConfiguration<
			SimpleAbstractState<
					Monolith,
					ValueEnvironment<IntInterval>,
					TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<
					Monolith,
					ValueEnvironment<IntInterval>,
					TypeEnvironment<TypeSet>>> conf;

	@BeforeClass
	public static void init() {
		LiSAConfiguration base = new LiSAConfiguration();
		base.glbThreshold = 5;
		base.wideningThreshold = 5;
		conf = new FixpointConfiguration<>(base);
	}

	private ModularWorstCaseAnalysis<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkAnalysis(
					Program p)
					throws InterproceduralAnalysisException,
					CallGraphConstructionException {
		ModularWorstCaseAnalysis<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>,
				SimpleAbstractDomain<Monolith,
						ValueEnvironment<IntInterval>,
						TypeEnvironment<TypeSet>>> analysis = new ModularWorstCaseAnalysis<>();
		RTACallGraph callgraph = new RTACallGraph();
		Application app = new Application(p);
		callgraph.init(app, null);
		analysis.init(
				app,
				callgraph,
				WorstCasePolicy.INSTANCE,
				null,
				new Analysis<>(DefaultConfiguration.defaultAbstractDomain()));
		return analysis;
	}

	private AnalysisState<
			SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> mkState() {
		return new AnalysisState<>(
				new ProgramState<>(DefaultConfiguration.defaultAbstractDomain().makeLattice(), new ExpressionSet()));
	}

	@Test
	public void testEmptyCFG()
			throws InterproceduralAnalysisException,
			CallGraphConstructionException,
			ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkAnalysis(p), new FIFOWorkingSet<>(), conf, new UniqueScope<>());
		} catch (FixpointException e) {
			System.err.println(e);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testEmptyIMPMethod()
			throws ParsingException,
			InterproceduralAnalysisException,
			CallGraphConstructionException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkAnalysis(p), new FIFOWorkingSet<>(), conf, new UniqueScope<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testIMPMethodWithEmptyIfBranch()
			throws ParsingException,
			InterproceduralAnalysisException,
			CallGraphConstructionException {
		Program p = IMPFrontend.processText("class empty { foo() { if (true) { this.foo(); } else {} } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkAnalysis(p), new FIFOWorkingSet<>(), conf, new UniqueScope<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testMetaVariablesOfRootExpressions()
			throws FixpointException,
			InterproceduralAnalysisException,
			CallGraphConstructionException {
		Program program = new Program(new IMPFeatures(), new IMPTypeSystem());
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "cfg"));
		OpenCall call = new OpenCall(cfg, SyntheticLocation.INSTANCE, CallType.STATIC, "test", "test");
		cfg.addNode(call, true);

		AnalysisState<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> domain = mkState();
		AnalyzedCFG<SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> result = cfg
				.fixpoint(domain, mkAnalysis(program), new FIFOWorkingSet<>(), conf, new UniqueScope<>());

		assertTrue(result.getAnalysisStateAfter(call).getExecutionState().valueState.getKeys().isEmpty());
	}

}
