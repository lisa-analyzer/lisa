package it.unive.lisa.program.cfg;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.WorstCasePolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import org.junit.Test;

public class CFGFixpointTest {

	private ModularWorstCaseAnalysis<
			SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
			MonolithicHeap,
			ValueEnvironment<Sign>,
			TypeEnvironment<InferredTypes>> mkAnalysis(Program p)
					throws InterproceduralAnalysisException, CallGraphConstructionException {
		ModularWorstCaseAnalysis<
				SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
				MonolithicHeap,
				ValueEnvironment<Sign>,
				TypeEnvironment<InferredTypes>> analysis = new ModularWorstCaseAnalysis<>();
		RTACallGraph callgraph = new RTACallGraph();
		Application app = new Application(p);
		callgraph.init(app);
		analysis.init(app, callgraph, WorstCasePolicy.INSTANCE);
		return analysis;
	}

	private AnalysisState<
			SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
			MonolithicHeap,
			ValueEnvironment<Sign>,
			TypeEnvironment<InferredTypes>> mkState() {
		return new AnalysisState<>(
				new SimpleAbstractState<>(
						new MonolithicHeap(),
						new ValueEnvironment<>(new Sign()),
						new TypeEnvironment<>(new InferredTypes())),
				new ExpressionSet<>(), new SymbolAliasing());
	}

	@Test
	public void testEmptyCFG()
			throws InterproceduralAnalysisException, CallGraphConstructionException, ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkAnalysis(p), FIFOWorkingSet.mk(), 5);
		} catch (FixpointException e) {
			System.err.println(e);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testEmptyIMPMethod()
			throws ParsingException, InterproceduralAnalysisException, CallGraphConstructionException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkAnalysis(p), FIFOWorkingSet.mk(), 5);
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testIMPMethodWithEmptyIfBranch()
			throws ParsingException, InterproceduralAnalysisException, CallGraphConstructionException {
		Program p = IMPFrontend.processText("class empty { foo() { if (true) { this.foo(); } else {} } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkAnalysis(p), FIFOWorkingSet.mk(), 5);
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testMetaVariablesOfRootExpressions()
			throws FixpointException, InterproceduralAnalysisException, CallGraphConstructionException {
		Program program = new Program(new IMPFeatures(), new IMPTypeSystem());
		CFG cfg = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "cfg"));
		OpenCall call = new OpenCall(cfg, SyntheticLocation.INSTANCE, CallType.STATIC, "test", "test");
		cfg.addNode(call, true);

		AnalysisState<
				SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
				MonolithicHeap,
				ValueEnvironment<Sign>,
				TypeEnvironment<InferredTypes>> domain = mkState();
		CFGWithAnalysisResults<
				SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
				MonolithicHeap,
				ValueEnvironment<Sign>,
				TypeEnvironment<InferredTypes>> result = cfg.fixpoint(domain,
						mkAnalysis(program), FIFOWorkingSet.mk(), 5);

		assertTrue(result.getAnalysisStateAfter(call).getState().getValueState().getKeys().isEmpty());
	}
}
