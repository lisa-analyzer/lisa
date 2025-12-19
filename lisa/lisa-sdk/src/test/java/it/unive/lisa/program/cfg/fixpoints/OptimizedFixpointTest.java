package it.unive.lisa.program.cfg.fixpoints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
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
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.optforward.OptimizedForwardFixpoint;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.Test;

public class OptimizedFixpointTest {

	private static class FixpointTester2
			extends
			OptimizedForwardFixpoint<TestAbstractState, TestAbstractDomain> {

		public FixpointTester2(
				CFG graph,
				boolean forceFullEvaluation,
				InterproceduralAnalysis<TestAbstractState, TestAbstractDomain> interprocedural,
				Predicate<Statement> hotspots) {
			super(graph, forceFullEvaluation, interprocedural, hotspots);
		}

		@Override
		public CompoundState<TestAbstractState> semantics(
				Statement node,
				CompoundState<TestAbstractState> entrystate)
				throws SemanticException {
			return entrystate;
		}

		@Override
		public CompoundState<TestAbstractState> traverse(
				Edge edge,
				CompoundState<TestAbstractState> entrystate)
				throws SemanticException {
			return entrystate;
		}

		@Override
		public CompoundState<TestAbstractState> union(
				Statement node,
				CompoundState<TestAbstractState> left,
				CompoundState<TestAbstractState> right)
				throws SemanticException {
			return left.merge(right);
		}

		@Override
		public CompoundState<TestAbstractState> join(
				Statement node,
				CompoundState<TestAbstractState> approx,
				CompoundState<TestAbstractState> old)
				throws SemanticException {
			return old.lub(approx);
		}

		@Override
		public boolean leq(
				Statement node,
				CompoundState<TestAbstractState> approx,
				CompoundState<TestAbstractState> old)
				throws SemanticException {
			return approx.lessOrEqual(old);
		}

		@Override
		public ForwardCFGFixpoint<TestAbstractState, TestAbstractDomain> mk(
				CFG graph,
				boolean forceFullEvaluation,
				InterproceduralAnalysis<TestAbstractState, TestAbstractDomain> interprocedural,
				FixpointConfiguration<TestAbstractState, TestAbstractDomain> config) {
			return new FixpointTester2(graph, forceFullEvaluation, interprocedural, st -> st instanceof Call);
		}

		@Override
		public BackwardCFGFixpoint<TestAbstractState, TestAbstractDomain> asBackward() {
			throw new UnsupportedOperationException();
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
			res = new FixpointTester2(graph, false, new TestInterproceduralAnalysis<>(), st -> st instanceof Call)
					.fixpoint(Map.of(source, comp), new FIFOWorkingSet<>());
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
			res = new FixpointTester2(graph, false, new TestInterproceduralAnalysis<>(), st -> st instanceof Call)
					.fixpoint(Map.of(source, comp), new FIFOWorkingSet<>());
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
			res = new FixpointTester2(graph, false, new TestInterproceduralAnalysis<>(), st -> st instanceof Call)
					.fixpoint(Map.of(source, comp), new FIFOWorkingSet<>());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}

		assertNotNull("Fixpoint failed", res);
		assertEquals("Fixpoint returned wrong result", Map.of(join, comp, left, comp, end, comp), res);
	}

}
