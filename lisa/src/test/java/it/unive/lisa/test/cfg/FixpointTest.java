package it.unive.lisa.test.cfg;

import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Test;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.callgraph.impl.intraproc.IntraproceduralCallGraph;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.test.imp.IMPFrontend;
import it.unive.lisa.test.imp.ParsingException;
import it.unive.lisa.util.datastructures.graph.FixpointException;

public class FixpointTest {

	private IntraproceduralCallGraph mkCallGraph() {
		return new IntraproceduralCallGraph();
	}

	private AnalysisState<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
			ValueEnvironment<Sign>> mkState() {
		return new AnalysisState<>(new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign())), Collections.emptyList());
	}
	
	@Test
	public void testEmptyCFG() {
		CFG cfg = new CFG(new CFGDescriptor(new CompilationUnit(null, 0, 0, "foo", false), false, "foo"));
		try {
			cfg.fixpoint(mkState(), mkCallGraph());
		} catch (FixpointException e) {
			System.err.println(e);
			fail("The fixpoint computation has thrown an exception");
		}
	}
	
	@Test
	public void testEmptyIMPMethod() throws ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkCallGraph());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}
	
	@Test
	public void testIMPMethodWithEmptyIfBranch() throws ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { if (true) { this.foo(); } else {} } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkCallGraph());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}
}
