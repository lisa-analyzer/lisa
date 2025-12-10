package it.unive.lisa.program.cfg.fixpoints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowExtractor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint.FixpointImplementation;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Map;
import org.junit.Test;

public class OptimizedFixpointTest {

	private static class FixpointTester2
			implements
			FixpointImplementation<Statement, Edge, CompoundState<TestAbstractState>> {

		@Override
		public CompoundState<TestAbstractState> semantics(
				Statement node,
				CompoundState<TestAbstractState> entrystate)
				throws Exception {
			return entrystate;
		}

		@Override
		public CompoundState<TestAbstractState> traverse(
				Edge edge,
				CompoundState<TestAbstractState> entrystate)
				throws Exception {
			return entrystate;
		}

		@Override
		public CompoundState<TestAbstractState> union(
				Statement node,
				CompoundState<TestAbstractState> left,
				CompoundState<TestAbstractState> right)
				throws Exception {
			return left.lub(right);
		}

		@Override
		public CompoundState<TestAbstractState> operation(
				Statement node,
				CompoundState<TestAbstractState> approx,
				CompoundState<TestAbstractState> old)
				throws Exception {
			return old.lub(approx);
		}

		@Override
		public boolean equality(
				Statement node,
				CompoundState<TestAbstractState> approx,
				CompoundState<TestAbstractState> old)
				throws Exception {
			return approx.lessOrEqual(old);
		}

	}

	@Test
	public void testLinearGraph() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		CodeUnit unit = new CodeUnit(SyntheticLocation.INSTANCE, program, "foo");
		CodeMemberDescriptor desc = new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "foo");
		CFG graph = new CFG(desc);
		Statement source = new VariableRef(graph, SyntheticLocation.INSTANCE, "x");
		Statement middle = new OpenCall(graph, SyntheticLocation.INSTANCE, CallType.STATIC, "foo", "bar");
		Statement end = new Ret(graph, SyntheticLocation.INSTANCE);
		graph.addNode(source, true);
		graph.addNode(middle);
		graph.addNode(end);
		graph.addEdge(new SequentialEdge(source, middle));
		graph.addEdge(new SequentialEdge(middle, end));
		graph.computeBasicBlocks();

		Map<Statement, CompoundState<TestAbstractState>> res = null;
		AnalysisState<TestAbstractState> state = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		CompoundState<TestAbstractState> comp = CompoundState.of(state.bottom(), new StatementStore<>(state.bottom()));
		try {
			res = new OptimizedFixpoint<TestAbstractState>(graph, false, st -> st instanceof Call)
					.fixpoint(Map.of(source, comp), new FIFOWorkingSet<>(), new FixpointTester2());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull("Fixpoint failed", res);
		assertEquals("Fixpoint returned wrong result", Map.of(middle, comp, end, comp), res);
	}

	@Test
	public void testBranchingGraph() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		CodeUnit unit = new CodeUnit(SyntheticLocation.INSTANCE, program, "foo");
		CodeMemberDescriptor desc = new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "foo");
		CFG graph = new CFG(desc);
		Statement source = new VariableRef(graph, SyntheticLocation.INSTANCE, "x");
		Statement left = new OpenCall(graph, SyntheticLocation.INSTANCE, CallType.STATIC, "foo", "bar");
		Statement right = new VariableRef(graph, SyntheticLocation.INSTANCE, "y");
		Statement join = new VariableRef(graph, SyntheticLocation.INSTANCE, "z");
		Statement end = new Ret(graph, SyntheticLocation.INSTANCE);
		graph.addNode(source, true);
		graph.addNode(left);
		graph.addNode(right);
		graph.addNode(join);
		graph.addNode(end);
		graph.addEdge(new TrueEdge(source, left));
		graph.addEdge(new FalseEdge(source, right));
		graph.addEdge(new SequentialEdge(left, join));
		graph.addEdge(new SequentialEdge(right, join));
		graph.addEdge(new SequentialEdge(join, end));
		graph.extractControlFlowStructures(new ControlFlowExtractor());
		graph.computeBasicBlocks();

		Map<Statement, CompoundState<TestAbstractState>> res = null;
		AnalysisState<TestAbstractState> state = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		CompoundState<TestAbstractState> comp = CompoundState.of(state.bottom(), new StatementStore<>(state.bottom()));
		try {
			res = new OptimizedFixpoint<TestAbstractState>(graph, false, st -> st instanceof Call)
					.fixpoint(Map.of(source, comp), new FIFOWorkingSet<>(), new FixpointTester2());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull("Fixpoint failed", res);
		assertEquals("Fixpoint returned wrong result", Map.of(left, comp, end, comp), res);
	}

	@Test
	public void testCyclicGraph() {
		Program program = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		CodeUnit unit = new CodeUnit(SyntheticLocation.INSTANCE, program, "foo");
		CodeMemberDescriptor desc = new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "foo");
		CFG graph = new CFG(desc);
		Statement source = new VariableRef(graph, SyntheticLocation.INSTANCE, "x");
		Statement left = new OpenCall(graph, SyntheticLocation.INSTANCE, CallType.STATIC, "foo", "bar");
		Statement right = new VariableRef(graph, SyntheticLocation.INSTANCE, "y");
		Statement join = new VariableRef(graph, SyntheticLocation.INSTANCE, "z");
		Statement end = new Ret(graph, SyntheticLocation.INSTANCE);
		graph.addNode(source, true);
		graph.addNode(left);
		graph.addNode(right);
		graph.addNode(join);
		graph.addNode(end);
		graph.addEdge(new SequentialEdge(source, join));
		graph.addEdge(new TrueEdge(join, left));
		graph.addEdge(new SequentialEdge(left, right));
		graph.addEdge(new SequentialEdge(right, join));
		graph.addEdge(new FalseEdge(join, end));
		graph.extractControlFlowStructures(new ControlFlowExtractor());
		graph.computeBasicBlocks();

		Map<Statement, CompoundState<TestAbstractState>> res = null;
		AnalysisState<TestAbstractState> state = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		CompoundState<TestAbstractState> comp = CompoundState.of(state.bottom(), new StatementStore<>(state.bottom()));
		try {
			res = new OptimizedFixpoint<TestAbstractState>(graph, false, st -> st instanceof Call)
					.fixpoint(Map.of(source, comp), new FIFOWorkingSet<>(), new FixpointTester2());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull("Fixpoint failed", res);
		assertEquals("Fixpoint returned wrong result", Map.of(join, comp, left, comp, end, comp), res);
	}

}
