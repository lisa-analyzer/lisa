package it.unive.lisa.analysis.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.numeric.NonRedundantIntervalSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NonRedundantIntervalsTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final NonRedundantIntervals domain = new NonRedundantIntervals();
	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable varAux = new Variable(Int32Type.INSTANCE, "aux", pp.getLocation());
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private NonRedundantIntervalSet mkSet(
			IntInterval... intervals) {
		return new NonRedundantIntervalSet(Set.of(intervals));
	}

	private NonRedundantIntervalSet mkSet(
			int low,
			int high) {
		return mkSet(new IntInterval(low, high));
	}

	private NonRedundantIntervalSet mkSingleton(
			int v) {
		return mkSet(new IntInterval(v, v));
	}

	private Constant mkConst(
			int val) {
		return new Constant(Int32Type.INSTANCE, val, pp.getLocation());
	}

	private ValueEnvironment<NonRedundantIntervalSet> stateWith(
			NonRedundantIntervalSet val) {
		return domain.makeLattice().putState(variable, val);
	}

	// --- evalConstant ---

	@Test
	void evalConstantLiftsToSingletonSet() throws SemanticException {
		NonRedundantIntervalSet result = domain.evalConstant(mkConst(5), pp, oracle);
		assertEquals(mkSingleton(5), result);
	}

	@Test
	void evalConstantNegative() throws SemanticException {
		NonRedundantIntervalSet result = domain.evalConstant(mkConst(-3), pp, oracle);
		assertEquals(mkSingleton(-3), result);
	}

	// --- evalUnaryExpression ---

	@Test
	void evalUnaryLiftedToEachElement() throws SemanticException {
		// negate each element: {[2,5]} → {[-5,-2]}
		NonRedundantIntervalSet arg = mkSet(2, 5);
		UnaryExpression expr = new UnaryExpression(Int32Type.INSTANCE, varAux, NumericNegation.INSTANCE,
				pp.getLocation());
		NonRedundantIntervalSet result = domain.evalUnaryExpression(expr, arg, pp, oracle);
		assertEquals(mkSet(-5, -2), result);
	}

	// --- evalBinaryExpression ---

	@Test
	void evalBinaryAddLiftsToCartesianProduct() throws SemanticException {
		// {[1,3]} + {[2,4]} = {[3,7]}
		NonRedundantIntervalSet left = mkSet(1, 3);
		NonRedundantIntervalSet right = mkSet(2, 4);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, varAux, variable, NumericNonOverflowingAdd.INSTANCE, pp.getLocation());
		NonRedundantIntervalSet result = domain.evalBinaryExpression(expr, left, right, pp, oracle);
		assertEquals(mkSet(3, 7), result);
	}

	// --- satisfiesBinaryExpression ---

	@Test
	void satisfiesGtConcrete() throws SemanticException {
		// {[5,5]} > {[3,3]} → SAT
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, varAux, variable, ComparisonGt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(expr, mkSingleton(5), mkSingleton(3), pp, oracle));
	}

	@Test
	void satisfiesGtTopReturnsUnknown() throws SemanticException {
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, varAux, variable, ComparisonGt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(expr, domain.top(), mkSingleton(3), pp, oracle));
	}

	// --- assumeBinaryExpression ---

	@Test
	void assumeEqRefinesIntervalSet() throws SemanticException {
		// x ∈ {[1,10]}, assume x == 5 → x ∈ {[5,5]}
		ValueEnvironment<NonRedundantIntervalSet> state = stateWith(mkSet(1, 10));
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(5), ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<NonRedundantIntervalSet> result = domain.assumeBinaryExpression(state, cond, pp, pp, oracle);
		assertEquals(mkSingleton(5), result.getState(variable));
	}

	@Test
	void assumeGtRefinesIntervalSet() throws SemanticException {
		// x ∈ {[3,7]}, assume x > 5 → x ∈ {[6,7]}
		ValueEnvironment<NonRedundantIntervalSet> state = stateWith(mkSet(3, 7));
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(5), ComparisonGt.INSTANCE, pp.getLocation());
		ValueEnvironment<NonRedundantIntervalSet> result = domain.assumeBinaryExpression(state, cond, pp, pp, oracle);
		assertEquals(mkSet(6, 7), result.getState(variable));
	}

	@Test
	void assumeContradictionReturnsBottom() throws SemanticException {
		// x ∈ {[7,10]}, assume x < 5 → bottom (no interval survives)
		ValueEnvironment<NonRedundantIntervalSet> state = stateWith(mkSet(7, 10));
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(5), ComparisonLt.INSTANCE, pp.getLocation());
		ValueEnvironment<NonRedundantIntervalSet> result = domain.assumeBinaryExpression(state, cond, pp, pp, oracle);
		assertTrue(result.isBottom());
	}

	@Test
	void assumeOnBottomStateReturnsBottom() throws SemanticException {
		// bottom state → bottom
		ValueEnvironment<NonRedundantIntervalSet> bottomState = domain.makeLattice().bottom();
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, variable, mkConst(5), ComparisonGt.INSTANCE, pp.getLocation());
		ValueEnvironment<
				NonRedundantIntervalSet> result = domain.assumeBinaryExpression(bottomState, cond, pp, pp, oracle);
		assertTrue(result.isBottom());
	}

	// --- constraints ---

	@Test
	void constraintsOnBottomStateReturnsNull() throws SemanticException {
		ValueEnvironment<NonRedundantIntervalSet> bottomState = domain.makeLattice().bottom();
		assertNull(domain.constraints(null, bottomState, variable, pp, oracle));
	}

	@Test
	void constraintsOnTopStateReturnsEmpty() throws SemanticException {
		ValueEnvironment<NonRedundantIntervalSet> topState = domain.makeLattice();
		assertTrue(domain.constraints(null, topState, variable, pp, oracle).isEmpty());
	}

	@Test
	void constraintsOnBoundedValueReturnsNonEmpty() throws SemanticException {
		// x ∈ {[2,8]}: should produce lower=2 and upper=8 constraints
		ValueEnvironment<NonRedundantIntervalSet> state = stateWith(mkSet(2, 8));
		Set<BinaryExpression> constraints = domain.constraints(null, state, variable, pp, oracle);
		assertNotNull(constraints);
		// Two range constraints: lower bound and upper bound
		assertEquals(2, constraints.size());
	}

	@Test
	void constraintsOnHalfBoundedProducesOneConstraint() throws SemanticException {
		// x ∈ {[-Inf,5]}: upper=5, lower=null → one constraint
		NonRedundantIntervalSet val = mkSet(new IntInterval(null, 5));
		ValueEnvironment<NonRedundantIntervalSet> state = stateWith(val);
		Set<BinaryExpression> constraints = domain.constraints(null, state, variable, pp, oracle);
		assertNotNull(constraints);
		assertEquals(1, constraints.size());
	}

	@Test
	void constraintsOnUnboundedValueReturnsEmpty() throws SemanticException {
		// x ∈ {[-Inf,+Inf]}: no finite bounds → empty set
		NonRedundantIntervalSet val = mkSet(IntInterval.TOP);
		ValueEnvironment<NonRedundantIntervalSet> state = stateWith(val);
		Set<BinaryExpression> constraints = domain.constraints(null, state, variable, pp, oracle);
		assertNotNull(constraints);
		assertTrue(constraints.isEmpty());
	}
}
