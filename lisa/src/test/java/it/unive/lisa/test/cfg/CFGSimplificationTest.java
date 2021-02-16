package it.unive.lisa.test.cfg;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.BinaryNativeCall;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Literal;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.UnaryNativeCall;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Untyped;
import org.junit.Test;

public class CFGSimplificationTest {

	@Test
	public void testSimpleSimplification() throws ProgramValidationException {
		CompilationUnit unit = new CompilationUnit(null, -1, -1, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unit, true, "foo"));
		Assignment assign = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first);
		Return ret = new Return(first, new VariableRef(first, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor(unit, true, "foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new VariableRef(second, "x"));

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
		CompilationUnit unit = new CompilationUnit(null, -1, -1, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unit, true, "foo"));
		Assignment assign = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop1 = new NoOp(first);
		NoOp noop2 = new NoOp(first);
		Return ret = new Return(first, new VariableRef(first, "x"));
		first.addNode(assign, true);
		first.addNode(noop1);
		first.addNode(noop2);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop1));
		first.addEdge(new SequentialEdge(noop1, noop2));
		first.addEdge(new SequentialEdge(noop2, ret));

		CFG second = new CFG(new CFGDescriptor(unit, true, "foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new VariableRef(second, "x"));

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
	public void testConditionalSimplification() throws ProgramValidationException {
		class GT extends BinaryNativeCall {
			protected GT(CFG cfg, Expression left, Expression right) {
				super(cfg, "gt", left, right);
			}

			@Override
			protected <A extends AbstractState<A, H, V>,
					H extends HeapDomain<H>,
					V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
							AnalysisState<A, H, V> entryState, CallGraph callGraph, AnalysisState<A, H, V> leftState,
							SymbolicExpression left,
							AnalysisState<A, H, V> rightState,
							SymbolicExpression right) throws SemanticException {
				return rightState;
			}
		}

		class Print extends UnaryNativeCall {
			protected Print(CFG cfg, Expression arg) {
				super(cfg, "print", arg);
			}

			@Override
			protected <A extends AbstractState<A, H, V>,
					H extends HeapDomain<H>,
					V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(
							AnalysisState<A, H, V> entryState, CallGraph callGraph, AnalysisState<A, H, V> exprState,
							SymbolicExpression expr)
							throws SemanticException {
				return entryState;
			}
		}

		CompilationUnit unit = new CompilationUnit(null, -1, -1, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unit, true, "foo"));
		Assignment assign = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		GT gt = new GT(first, new VariableRef(first, "x"), new Literal(first, 2, Untyped.INSTANCE));
		Print print = new Print(first, new Literal(first, "f", Untyped.INSTANCE));
		NoOp noop1 = new NoOp(first);
		NoOp noop2 = new NoOp(first);
		Return ret = new Return(first, new VariableRef(first, "x", Untyped.INSTANCE));
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

		CFG second = new CFG(new CFGDescriptor(unit, true, "foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		gt = new GT(second, new VariableRef(second, "x"), new Literal(second, 2, Untyped.INSTANCE));
		print = new Print(second, new Literal(second, "f", Untyped.INSTANCE));
		ret = new Return(second, new VariableRef(second, "x", Untyped.INSTANCE));

		second.addNode(assign, true);
		second.addNode(gt);
		second.addNode(print);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, gt));
		second.addEdge(new TrueEdge(gt, print));
		second.addEdge(new FalseEdge(gt, ret));
		second.addEdge(new SequentialEdge(print, ret));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationWithDuplicateStatements() throws ProgramValidationException {
		CompilationUnit unit = new CompilationUnit(null, -1, -1, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unit, true, "foo"));
		Assignment assign = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first);
		Return ret = new Return(first, new VariableRef(first, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor(unit, true, "foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new VariableRef(first, "x"));

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
		CompilationUnit unit = new CompilationUnit(null, -1, -1, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unit, false, "foo"));
		NoOp start = new NoOp(first);
		Assignment assign = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		Return ret = new Return(first, new VariableRef(first, "x"));
		first.addNode(start, true);
		first.addNode(assign);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, ret));
		first.addEdge(new SequentialEdge(start, assign));

		CFG second = new CFG(new CFGDescriptor(unit, false, "foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new VariableRef(first, "x"));

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
		CompilationUnit unit = new CompilationUnit(null, -1, -1, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unit, false, "foo"));
		Assignment assign1 = new Assignment(first, new VariableRef(first, "x"),
				new Literal(first, 5, Untyped.INSTANCE));
		Assignment assign2 = new Assignment(first, new VariableRef(first, "y"),
				new Literal(first, 50, Untyped.INSTANCE));
		NoOp end = new NoOp(first);
		first.addNode(assign1, true);
		first.addNode(assign2);
		first.addNode(end);
		first.addEdge(new SequentialEdge(assign1, assign2));
		first.addEdge(new SequentialEdge(assign2, end));

		CFG second = new CFG(new CFGDescriptor(unit, false, "foo"));
		assign1 = new Assignment(second, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		assign2 = new Assignment(second, new VariableRef(first, "y"), new Literal(first, 50, Untyped.INSTANCE));

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
		CompilationUnit unit = new CompilationUnit(null, -1, -1, "foo", false);
		CFG first = new CFG(new CFGDescriptor(unit, false, "foo"));
		Assignment assign1 = new Assignment(first, new VariableRef(first, "b"),
				new Literal(first, true, Untyped.INSTANCE));
		Assignment assign2 = new Assignment(first, new VariableRef(first, "x"),
				new Literal(first, 5, Untyped.INSTANCE));
		Assignment assign3 = new Assignment(first, new VariableRef(first, "y"),
				new Literal(first, 50, Untyped.INSTANCE));
		NoOp end = new NoOp(first);
		first.addNode(end);
		first.addNode(assign1, true);
		first.addNode(assign2);
		first.addNode(assign3);
		first.addEdge(new TrueEdge(assign1, assign2));
		first.addEdge(new FalseEdge(assign1, assign3));
		first.addEdge(new SequentialEdge(assign2, end));
		first.addEdge(new SequentialEdge(assign3, end));

		CFG second = new CFG(new CFGDescriptor(unit, false, "foo"));
		assign1 = new Assignment(second, new VariableRef(first, "b"), new Literal(second, true, Untyped.INSTANCE));
		assign2 = new Assignment(second, new VariableRef(first, "x"), new Literal(second, 5, Untyped.INSTANCE));
		assign3 = new Assignment(second, new VariableRef(first, "y"), new Literal(second, 50, Untyped.INSTANCE));
		second.addNode(assign1, true);
		second.addNode(assign2);
		second.addNode(assign3);
		second.addEdge(new TrueEdge(assign1, assign2));
		second.addEdge(new FalseEdge(assign1, assign3));

		first.validate();
		second.validate();
		first.simplify();
		first.validate();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}
}
