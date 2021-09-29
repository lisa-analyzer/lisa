package it.unive.lisa.program.cfg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.BinaryNativeCall;
import it.unive.lisa.program.cfg.statement.call.UnaryNativeCall;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.AdjacencyMatrix;
import org.junit.Test;

public class CFGSimplificationTest {

	@Test
	public void testSimpleSimplification() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CompilationUnit unit = new CompilationUnit(unknown, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unknown, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new Literal(first, unknown, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first, unknown);
		Return ret = new Return(first, unknown,
				new VariableRef(first, unknown, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor(unknown, unit, true, "foo"));
		assign = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new Literal(second, unknown, 5, Untyped.INSTANCE));
		ret = new Return(second, unknown, new VariableRef(second, unknown, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testDoubleSimplification() throws ProgramValidationException {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("fake", 0, 0);
		SourceCodeLocation unknownLocation2 = new SourceCodeLocation("fake", 0, 1);
		CompilationUnit unit = new CompilationUnit(unknownLocation, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unknownLocation, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknownLocation, new VariableRef(first, unknownLocation, "x"),
				new Literal(first, unknownLocation, 5, Untyped.INSTANCE));
		NoOp noop1 = new NoOp(first, unknownLocation);
		NoOp noop2 = new NoOp(first, unknownLocation2);
		Return ret = new Return(first, unknownLocation, new VariableRef(first, unknownLocation, "x"));
		first.addNode(assign, true);
		first.addNode(noop1);
		first.addNode(noop2);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop1));
		first.addEdge(new SequentialEdge(noop1, noop2));
		first.addEdge(new SequentialEdge(noop2, ret));

		CFG second = new CFG(new CFGDescriptor(unknownLocation, unit, true, "foo"));
		assign = new Assignment(second, unknownLocation,
				new VariableRef(second, unknownLocation, "x"),
				new Literal(second, unknownLocation, 5, Untyped.INSTANCE));
		ret = new Return(second, unknownLocation, new VariableRef(second, unknownLocation, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	private static class GT extends BinaryNativeCall {
		protected GT(CFG cfg, Expression left, Expression right) {
			super(cfg, new SourceCodeLocation("unknown", 0, 0), "gt", left, right);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
						AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
						AnalysisState<A, H, V> leftState, SymbolicExpression left,
						AnalysisState<A, H, V> rightState, SymbolicExpression right) throws SemanticException {
			return rightState;
		}
	}

	private static class Print extends UnaryNativeCall {
		protected Print(CFG cfg, Expression arg) {
			super(cfg, new SourceCodeLocation("unknown", 0, 0), "print", arg);
		}

		@Override
		protected <A extends AbstractState<A, H, V>,
				H extends HeapDomain<H>,
				V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(
						AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
						AnalysisState<A, H, V> exprState, SymbolicExpression expr)
						throws SemanticException {
			return entryState;
		}

	}

	@Test
	public void testConditionalSimplification() throws ProgramValidationException {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("fake", 0, 0);
		SourceCodeLocation unknownLocation2 = new SourceCodeLocation("fake", 0, 1);
		CompilationUnit unit = new CompilationUnit(unknownLocation, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unknownLocation, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknownLocation, new VariableRef(first, unknownLocation, "x"),
				new Literal(first, unknownLocation, 5, Untyped.INSTANCE));
		GT gt = new GT(first, new VariableRef(first, unknownLocation, "x"),
				new Literal(first, unknownLocation, 2, Untyped.INSTANCE));
		Print print = new Print(first, new Literal(first, unknownLocation, "f", Untyped.INSTANCE));
		NoOp noop1 = new NoOp(first, unknownLocation);
		NoOp noop2 = new NoOp(first, unknownLocation2);
		Return ret = new Return(first, unknownLocation, new VariableRef(first, unknownLocation, "x", Untyped.INSTANCE));
		first.addNode(assign, true);
		first.addNode(gt);
		first.addNode(print);
		first.addNode(noop1);
		first.addNode(noop2);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, gt));
		first.addEdge(new TrueEdge(gt, print));
		first.addEdge(new FalseEdge(gt, noop1));
		first.addEdge(new SequentialEdge(noop1, noop2));
		first.addEdge(new SequentialEdge(print, noop2));
		first.addEdge(new SequentialEdge(noop2, ret));

		AdjacencyMatrix<Statement, Edge, CFG> tbranch = new AdjacencyMatrix<>();
		tbranch.addNode(print);
		AdjacencyMatrix<Statement, Edge, CFG> fbranch = new AdjacencyMatrix<>();
		tbranch.addNode(noop1);
		first.addControlFlowStructure(
				new IfThenElse(first.getAdjacencyMatrix(), gt, noop2, tbranch.getNodes(), fbranch.getNodes()));

		CFG second = new CFG(new CFGDescriptor(unknownLocation, unit, true, "foo"));
		assign = new Assignment(second, unknownLocation,
				new VariableRef(second, unknownLocation, "x"),
				new Literal(second, unknownLocation, 5, Untyped.INSTANCE));
		gt = new GT(second, new VariableRef(second, unknownLocation, "x"),
				new Literal(second, unknownLocation, 2, Untyped.INSTANCE));
		print = new Print(second, new Literal(second, unknownLocation, "f", Untyped.INSTANCE));
		ret = new Return(second, unknownLocation,
				new VariableRef(second, unknownLocation, "x", Untyped.INSTANCE));

		second.addNode(assign, true);
		second.addNode(gt);
		second.addNode(print);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, gt));
		second.addEdge(new TrueEdge(gt, print));
		second.addEdge(new FalseEdge(gt, ret));
		second.addEdge(new SequentialEdge(print, ret));

		tbranch = new AdjacencyMatrix<>();
		tbranch.addNode(print);
		fbranch = new AdjacencyMatrix<>();
		second.addControlFlowStructure(
				new IfThenElse(second.getAdjacencyMatrix(), gt, ret, tbranch.getNodes(), fbranch.getNodes()));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
		ControlFlowStructure exp = second.getControlFlowStructures().iterator().next();
		ControlFlowStructure act = first.getControlFlowStructures().iterator().next();
		assertEquals("Simplification did not update control flow structures", exp, act);
	}

	@Test
	public void testSimplificationWithDuplicateStatements() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CompilationUnit unit = new CompilationUnit(unknown, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unknown, unit, true, "foo"));
		Assignment assign = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new Literal(first, unknown, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first, unknown);
		Return ret = new Return(first, unknown,
				new VariableRef(first, unknown, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor(unknown, unit, true, "foo"));
		assign = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new Literal(second, unknown, 5, Untyped.INSTANCE));
		ret = new Return(second, unknown, new VariableRef(first, unknown, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationAtTheStart() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CompilationUnit unit = new CompilationUnit(unknown, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unknown, unit, false, "foo"));
		NoOp start = new NoOp(first, unknown);
		Assignment assign = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new Literal(first, unknown, 5, Untyped.INSTANCE));
		Return ret = new Return(first, unknown,
				new VariableRef(first, unknown, "x"));
		first.addNode(start, true);
		first.addNode(assign);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, ret));
		first.addEdge(new SequentialEdge(start, assign));

		CFG second = new CFG(new CFGDescriptor(unknown, unit, false, "foo"));
		assign = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new Literal(second, unknown, 5, Untyped.INSTANCE));
		ret = new Return(second, unknown, new VariableRef(first, unknown, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationAtTheEnd() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CompilationUnit unit = new CompilationUnit(unknown, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unknown, unit, false, "foo"));
		Assignment assign1 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new Literal(first, unknown, 5, Untyped.INSTANCE));
		Assignment assign2 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "y"),
				new Literal(first, unknown, 50, Untyped.INSTANCE));
		NoOp end = new NoOp(first, unknown);
		first.addNode(assign1, true);
		first.addNode(assign2);
		first.addNode(end);
		first.addEdge(new SequentialEdge(assign1, assign2));
		first.addEdge(new SequentialEdge(assign2, end));

		CFG second = new CFG(new CFGDescriptor(unknown, unit, false, "foo"));
		assign1 = new Assignment(second, unknown,
				new VariableRef(first, unknown, "x"),
				new Literal(first, unknown, 5, Untyped.INSTANCE));
		assign2 = new Assignment(second, unknown,
				new VariableRef(first, unknown, "y"),
				new Literal(first, unknown, 50, Untyped.INSTANCE));

		second.addNode(assign1, true);
		second.addNode(assign2);

		second.addEdge(new SequentialEdge(assign1, assign2));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationAtTheEndWithBranch() throws ProgramValidationException {
		SourceCodeLocation unknown = new SourceCodeLocation("unknown", 0, 0);
		CompilationUnit unit = new CompilationUnit(unknown, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unknown, unit, false, "foo"));
		Assignment assign1 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "b"),
				new Literal(first, unknown, true, Untyped.INSTANCE));
		Assignment assign2 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "x"),
				new Literal(first, unknown, 5, Untyped.INSTANCE));
		Assignment assign3 = new Assignment(first, unknown,
				new VariableRef(first, unknown, "y"),
				new Literal(first, unknown, 50, Untyped.INSTANCE));
		NoOp end = new NoOp(first, unknown);
		first.addNode(end);
		first.addNode(assign1, true);
		first.addNode(assign2);
		first.addNode(assign3);
		first.addEdge(new TrueEdge(assign1, assign2));
		first.addEdge(new FalseEdge(assign1, assign3));
		first.addEdge(new SequentialEdge(assign2, end));
		first.addEdge(new SequentialEdge(assign3, end));

		AdjacencyMatrix<Statement, Edge, CFG> tbranch = new AdjacencyMatrix<>();
		tbranch.addNode(assign2);
		AdjacencyMatrix<Statement, Edge, CFG> fbranch = new AdjacencyMatrix<>();
		fbranch.addNode(assign3);
		first.addControlFlowStructure(
				new IfThenElse(first.getAdjacencyMatrix(), assign1, end, tbranch.getNodes(), fbranch.getNodes()));

		CFG second = new CFG(new CFGDescriptor(unknown, unit, false, "foo"));
		assign1 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "b"),
				new Literal(second, unknown, true, Untyped.INSTANCE));
		assign2 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "x"),
				new Literal(second, unknown, 5, Untyped.INSTANCE));
		assign3 = new Assignment(second, unknown,
				new VariableRef(second, unknown, "y"),
				new Literal(second, unknown, 50, Untyped.INSTANCE));
		second.addNode(assign1, true);
		second.addNode(assign2);
		second.addNode(assign3);
		second.addEdge(new TrueEdge(assign1, assign2));
		second.addEdge(new FalseEdge(assign1, assign3));

		tbranch = new AdjacencyMatrix<>();
		tbranch.addNode(assign2);
		fbranch = new AdjacencyMatrix<>();
		fbranch.addNode(assign3);
		second.addControlFlowStructure(
				new IfThenElse(second.getAdjacencyMatrix(), assign1, null, tbranch.getNodes(), fbranch.getNodes()));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
		ControlFlowStructure exp = second.getControlFlowStructures().iterator().next();
		ControlFlowStructure act = first.getControlFlowStructures().iterator().next();
		assertEquals("Simplification did not update control flow structures", exp, act);
	}
}
