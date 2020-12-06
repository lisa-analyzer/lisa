package it.unive.lisa.test.cfg;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFGDescriptor;
import it.unive.lisa.cfg.edge.FalseEdge;
import it.unive.lisa.cfg.edge.SequentialEdge;
import it.unive.lisa.cfg.edge.TrueEdge;
import it.unive.lisa.cfg.statement.Assignment;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Literal;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.cfg.statement.NoOp;
import it.unive.lisa.cfg.statement.Return;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.cfg.type.Untyped;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Collection;
import org.junit.Test;

public class CFGSimplificationTest {

	@Test
	public void testSimpleSimplification() {
		CFG first = new CFG(new CFGDescriptor("foo"));
		Assignment assign = new Assignment(first, new Variable(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first);
		Return ret = new Return(first, new Variable(first, "x"));
		first.addNode(assign, true);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor("foo"));
		assign = new Assignment(second, new Variable(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new Variable(second, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testDoubleSimplification() {
		CFG first = new CFG(new CFGDescriptor("foo"));
		Assignment assign = new Assignment(first, new Variable(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop1 = new NoOp(first);
		NoOp noop2 = new NoOp(first);
		Return ret = new Return(first, new Variable(first, "x"));
		first.addNode(assign, true);
		first.addNode(noop1);
		first.addNode(noop2);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop1));
		first.addEdge(new SequentialEdge(noop1, noop2));
		first.addEdge(new SequentialEdge(noop2, ret));

		CFG second = new CFG(new CFGDescriptor("foo"));
		assign = new Assignment(second, new Variable(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Return(second, new Variable(second, "x"));

		second.addNode(assign, true);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}

	@Test
	public void testConditionalSimplification() {
		class GT extends NativeCall {
			protected GT(CFG cfg, Expression left, Expression right) {
				super(cfg, "gt", left, right);
			}

			@Override
			public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
					AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
					throws SemanticException {
				return computedState;
			}

			@Override
			public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> callTypeInference(
					AnalysisState<H, TypeEnvironment> computedState,
					CallGraph callGraph, Collection<SymbolicExpression>[] params) throws SemanticException {
				return computedState;
			}
		}

		class Print extends NativeCall {
			protected Print(CFG cfg, Expression arg) {
				super(cfg, "print", arg);
			}

			@Override
			public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
					AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
					throws SemanticException {
				return computedState;
			}

			@Override
			public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> callTypeInference(
					AnalysisState<H, TypeEnvironment> computedState,
					CallGraph callGraph, Collection<SymbolicExpression>[] params) throws SemanticException {
				return computedState;
			}
		}

		CFG first = new CFG(new CFGDescriptor("foo"));
		Assignment assign = new Assignment(first, new Variable(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		GT gt = new GT(first, new Variable(first, "x"), new Literal(first, 2, Untyped.INSTANCE));
		Print print = new Print(first, new Literal(first, "f", Untyped.INSTANCE));
		NoOp noop1 = new NoOp(first);
		NoOp noop2 = new NoOp(first);
		Return ret = new Return(first, new Variable(first, "x", Untyped.INSTANCE));
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
		assign = new Assignment(second, new Variable(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		gt = new GT(second, new Variable(second, "x"), new Literal(second, 2, Untyped.INSTANCE));
		print = new Print(second, new Literal(second, "f", Untyped.INSTANCE));
		ret = new Return(second, new Variable(second, "x", Untyped.INSTANCE));

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
		Assignment assign = new Assignment(first, new Variable(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		NoOp noop = new NoOp(first);
		Assignment ret = new Assignment(first, new Variable(first, "x"), new Literal(first, 5, Untyped.INSTANCE));
		first.addNode(assign);
		first.addNode(noop);
		first.addNode(ret);
		first.addEdge(new SequentialEdge(assign, noop));
		first.addEdge(new SequentialEdge(noop, ret));

		CFG second = new CFG(new CFGDescriptor("foo"));
		assign = new Assignment(second, new Variable(second, "x"), new Literal(second, 5, Untyped.INSTANCE));
		ret = new Assignment(first, new Variable(first, "x"), new Literal(first, 5, Untyped.INSTANCE));

		second.addNode(assign);
		second.addNode(ret);

		second.addEdge(new SequentialEdge(assign, ret));

		first.simplify();
		assertTrue("Different CFGs", second.isEqualTo(first));
	}
}
