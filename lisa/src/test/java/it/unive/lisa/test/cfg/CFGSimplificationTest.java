package it.unive.lisa.test.cfg;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
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
	public void testSimpleSimplification() {
		CFG first = new CFG(new CFGDescriptor("foo"));
		Assignment assign = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first);
		Return ret = new Return(first, new VariableRef(first, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor("foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new VariableRef(second, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testDoubleSimplification() {
		CFG first = new CFG(new CFGDescriptor("foo"));
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

		CFG second = new CFG(new CFGDescriptor("foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new VariableRef(second, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testConditionalSimplification() {
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

		CFG first = new CFG(new CFGDescriptor("foo"));
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

		CFG second = new CFG(new CFGDescriptor("foo"));
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

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testSimplificationWithDuplicateStatements() {
		CFG first = new CFG(new CFGDescriptor("foo"));
		Assignment assign = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first);
		Assignment ret = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		first.addNode(assign);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor("foo"));
		assign = new Assignment(second, new VariableRef(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Assignment(first, new VariableRef(first, "x"), new Literal(first, 5, Untyped.INSTANCE));

		second.addNode(assign);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}
}
