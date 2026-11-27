package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.interprocedural;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.store;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link UnaryExpression}, {@link BinaryExpression} and
 * {@link TernaryExpression} all implement
 * {@code forwardSemanticsAux}/{@code backwardSemanticsAux} as a Cartesian
 * product over the computed values of their sub-expressions, lub-ing the result
 * of one {@code fwd*Semantics}/{@code bwd*Semantics} call per combination.
 * These tests isolate exactly that dispatch logic, independently of
 * {@link NaryExpression}'s evaluation-order orchestration (covered by
 * {@link NaryExpressionTest}), by invoking {@code forwardSemanticsAux} directly
 * with hand-built {@link ExpressionSet}s.
 */
public class UnaryBinaryTernaryExpressionTest {

	private static SymbolicExpression var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, LOC);
	}

	private static AnalysisState<TestAbstractState> state() {
		return new AnalysisState<>(new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
	}

	private static class CountingUnary
			extends
			UnaryExpression {

		int calls = 0;

		CountingUnary(
				CFG cfg,
				CodeLocation loc,
				Expression sub) {
			super(cfg, loc, "u", sub);
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				SymbolicExpression expr,
				StatementStore<A> expressions) {
			calls++;
			return state;
		}
	}

	private static class CountingBinary
			extends
			BinaryExpression {

		int calls = 0;

		CountingBinary(
				CFG cfg,
				CodeLocation loc,
				Expression left,
				Expression right) {
			super(cfg, loc, "b", left, right);
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				SymbolicExpression left,
				SymbolicExpression right,
				StatementStore<A> expressions) {
			calls++;
			return state;
		}
	}

	private static class CountingTernary
			extends
			TernaryExpression {

		int calls = 0;

		CountingTernary(
				CFG cfg,
				CodeLocation loc,
				Expression left,
				Expression middle,
				Expression right) {
			super(cfg, loc, "t", left, middle, right);
		}

		@Override
		protected int compareSameClassAndParams(
				Statement o) {
			return 0;
		}

		@Override
		public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
				InterproceduralAnalysis<A, D> interprocedural,
				AnalysisState<A> state,
				SymbolicExpression left,
				SymbolicExpression middle,
				SymbolicExpression right,
				StatementStore<A> expressions) {
			calls++;
			return state;
		}
	}

	@Test
	public void unaryDispatchesOnceForEachComputedValueOfItsSubExpression()
			throws Exception {
		CFG cfg = newCfg();
		CountingUnary u = new CountingUnary(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		ExpressionSet[] params = { new ExpressionSet(Set.of(var("a"), var("b"))) };
		AnalysisState<TestAbstractState> s = state();
		u.forwardSemanticsAux(interprocedural(), s, params, store(s));
		assertEquals(2, u.calls);
		assertSame(u.getSubExpression(), u.getSubExpressions()[0]);
	}

	@Test
	public void binaryDispatchesOnceForEachCombinationOfBothSubExpressions()
			throws Exception {
		CFG cfg = newCfg();
		CountingBinary b = new CountingBinary(cfg, LOC, new VariableRef(cfg, LOC, "x"), new VariableRef(cfg, LOC, "y"));
		ExpressionSet[] params = {
				new ExpressionSet(Set.of(var("a"), var("b"))),
				new ExpressionSet(Set.of(var("c"), var("d"), var("e"))) };
		AnalysisState<TestAbstractState> s = state();
		b.forwardSemanticsAux(interprocedural(), s, params, store(s));
		assertEquals(2 * 3, b.calls);
		assertSame(b.getSubExpressions()[0], b.getLeft());
		assertSame(b.getSubExpressions()[1], b.getRight());
	}

	@Test
	public void ternaryDispatchesOnceForEachCombinationOfAllThreeSubExpressions()
			throws Exception {
		CFG cfg = newCfg();
		CountingTernary t = new CountingTernary(
				cfg, LOC, new VariableRef(cfg, LOC, "x"), new VariableRef(cfg, LOC, "y"),
				new VariableRef(cfg, LOC, "z"));
		ExpressionSet[] params = {
				new ExpressionSet(var("a")),
				new ExpressionSet(Set.of(var("b"), var("c"))),
				new ExpressionSet(Set.of(var("d"), var("e"))) };
		AnalysisState<TestAbstractState> s = state();
		t.forwardSemanticsAux(interprocedural(), s, params, store(s));
		assertEquals(1 * 2 * 2, t.calls);
		assertSame(t.getSubExpressions()[0], t.getLeft());
		assertSame(t.getSubExpressions()[1], t.getMiddle());
		assertSame(t.getSubExpressions()[2], t.getRight());
	}

	@Test
	public void backwardSemanticsDefaultsToForwardSemanticsForAllThreeArities()
			throws Exception {
		CFG cfg = newCfg();
		CountingUnary u = new CountingUnary(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		AnalysisState<TestAbstractState> s = state();
		u.forwardSemanticsAux(interprocedural(), s, new ExpressionSet[] { new ExpressionSet(var("a")) }, store(s));
		int afterFwd = u.calls;
		u.backwardSemanticsAux(interprocedural(), s, new ExpressionSet[] { new ExpressionSet(var("a")) }, store(s));
		assertEquals(afterFwd + 1, u.calls);
	}

}
