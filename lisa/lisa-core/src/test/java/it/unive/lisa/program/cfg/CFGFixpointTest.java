package it.unive.lisa.program.cfg;

import static org.junit.Assert.fail;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import org.junit.Test;

public class CFGFixpointTest {

	private ModularWorstCaseAnalysis<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
			ValueEnvironment<Sign>> mkAnalysis(Program p)
					throws InterproceduralAnalysisException, CallGraphConstructionException {
		ModularWorstCaseAnalysis<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
				ValueEnvironment<Sign>> analysis = new ModularWorstCaseAnalysis<>();
		RTACallGraph callgraph = new RTACallGraph();
		callgraph.init(p);
		analysis.init(p, callgraph);
		return analysis;
	}

	private AnalysisState<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
			ValueEnvironment<Sign>> mkState() {
		return new AnalysisState<>(new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign())),
				new ExpressionSet<>());
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
}
