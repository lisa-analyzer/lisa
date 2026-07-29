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
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class UpperBoundsTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final UpperBounds domain = new UpperBounds();
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final Variable x = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable y = new Variable(Int32Type.INSTANCE, "y", pp.getLocation());
	private final Variable z = new Variable(Int32Type.INSTANCE, "z", pp.getLocation());

	private Constant mkConst(
			int v) {
		return new Constant(Int32Type.INSTANCE, v, pp.getLocation());
	}

	/** Creates a state where `small < big`. */
	private ValueEnvironment<DefiniteIdSet> stateWithBound(
			Variable small,
			Variable big) {
		return domain.makeLattice().putState(small, new DefiniteIdSet(Set.of(big)));
	}

	// --- assign: subtraction id = y - c ---

	@Test
	void assignSubtractPositiveConstantMakesIdLessThanY() throws SemanticException {
		// id = y - 3 → id < y because c > 0
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, y, mkConst(3), NumericNonOverflowingSub.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assign(state, x, expr, pp, oracle);
		assertTrue(result.getState(x).contains(y));
	}

	@Test
	void assignSubtractNegativeConstantMakesYLessThanId() throws SemanticException {
		// id = y - (-3) = y + 3 → y < id because −c > 0
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, y, mkConst(-3), NumericNonOverflowingSub.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assign(state, x, expr, pp, oracle);
		assertTrue(result.getState(y).contains(x));
	}

	// --- assign: addition id = y + c ---

	@Test
	void assignAddPositiveConstantMakesYLessThanId() throws SemanticException {
		// id = y + 3 → y < id because c > 0
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, y, mkConst(3), NumericNonOverflowingAdd.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assign(state, x, expr, pp, oracle);
		assertTrue(result.getState(y).contains(x));
	}

	@Test
	void assignAddNegativeConstantMakesIdLessThanY() throws SemanticException {
		// id = y + (-3) = y - 3 → id < y because c < 0
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, y, mkConst(-3), NumericNonOverflowingAdd.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assign(state, x, expr, pp, oracle);
		assertTrue(result.getState(x).contains(y));
	}

	// --- assign: cleanup ---

	@Test
	void assignRemovesReassignedVariableFromOtherBounds() throws SemanticException {
		// y < x is known; after reassigning x, x should no longer appear in y's
		// bounds
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(y, x);
		ValueEnvironment<DefiniteIdSet> result = domain.assign(state, x, mkConst(5), pp, oracle);
		assertTrue(!result.getState(y).contains(x));
	}

	// --- satisfies ---

	@Test
	void satisfiesLtWhenUpperBoundKnown() throws SemanticException {
		// x < y is in state → x < y is SATISFIED
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesLtWhenUpperBoundNotKnown() throws SemanticException {
		// no bound info → x < y is UNKNOWN
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.UNKNOWN, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesLeWhenUpperBoundKnown() throws SemanticException {
		// x < y in state → x <= y is at least SATISFIED
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLe.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesLeWhenUpperBoundNotKnown() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLe.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.UNKNOWN, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesGtWhenLowerBoundKnown() throws SemanticException {
		// y < x is in state → x > y is SATISFIED
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(y, x);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesGtWhenNotKnown() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.UNKNOWN, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesGeWhenLowerBoundKnown() throws SemanticException {
		// y < x → x >= y is at least SATISFIED
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(y, x);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGe.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesEqReturnsUnknown() throws SemanticException {
		// EQ is not handled by UpperBounds → always UNKNOWN
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonEq.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.UNKNOWN, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesNonIdentifierOperandsReturnsUnknown() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, mkConst(5), ComparisonLt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.UNKNOWN, domain.satisfies(state, expr, pp, oracle));
	}

	// --- assume ---

	@Test
	void assumeLtAddsUpperBound() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(state, cond, pp, pp, oracle);
		// After x < y, y should be in x's upper-bound set
		assertTrue(result.getState(x).contains(y));
	}

	@Test
	void assumeLtPreservesExistingBounds() throws SemanticException {
		// x < z is already known; after x < y, x should have both y and z
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, z);
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(state, cond, pp, pp, oracle);
		assertTrue(result.getState(x).contains(y));
		assertTrue(result.getState(x).contains(z));
	}

	@Test
	void assumeGtAddsUpperBoundOnRightSide() throws SemanticException {
		// x > y → y < x, so x should be in y's upper-bound set
		ValueEnvironment<DefiniteIdSet> state = domain.makeLattice();
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGt.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(state, cond, pp, pp, oracle);
		assertTrue(result.getState(y).contains(x));
	}

	@Test
	void assumeLePropagatesBoundsFromRightOperand() throws SemanticException {
		// y < z is known; assume x <= y → x should inherit z (x <= y < z → x <
		// z)
		// LE does not add a strict bound for y itself (x <= y doesn't imply x <
		// y)
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(y, z);
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLe.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(state, cond, pp, pp, oracle);
		assertTrue(result.getState(x).contains(z));
	}

	@Test
	void assumeGePropagatesBoundsFromLeftOperand() throws SemanticException {
		// x < z is known; assume x >= y → y inherits z (y <= x < z → y < z)
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, z);
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGe.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(state, cond, pp, pp, oracle);
		assertTrue(result.getState(y).contains(z));
	}

	@Test
	void assumeEqMergesBothSides() throws SemanticException {
		// x == y; x already knows x < z; after eq, y should also know x < z
		// (merged bounds)
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, z);
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonEq.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(state, cond, pp, pp, oracle);
		// merged bounds: both x and y should have z
		assertTrue(result.getState(x).contains(z));
		assertTrue(result.getState(y).contains(z));
	}

	@Test
	void assumeNonIdentifierOperandsLeavesStateUnchanged() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, mkConst(5), ComparisonLt.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(state, cond, pp, pp, oracle);
		assertEquals(state, result);
	}

	@Test
	void assumeOnBottomReturnsBottom() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> bottom = domain.makeLattice().bottom();
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		ValueEnvironment<DefiniteIdSet> result = domain.assume(bottom, cond, pp, pp, oracle);
		assertTrue(result.isBottom());
	}

	// --- constraints ---

	@Test
	void constraintsOnBottomReturnsNull() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> bottom = domain.makeLattice().bottom();
		assertNull(domain.constraints(null, bottom, x, pp, oracle));
	}

	@Test
	void constraintsOnTopStateReturnsEmpty() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> top = domain.makeLattice();
		assertTrue(domain.constraints(null, top, x, pp, oracle).isEmpty());
	}

	@Test
	void constraintsWithUpperBoundReturnsConstraint() throws SemanticException {
		// x < y (y ∈ x's bounds) → constraint: y > x
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		Set<BinaryExpression> constraints = domain.constraints(null, state, x, pp, oracle);
		assertNotNull(constraints);
		assertEquals(1, constraints.size());
		BinaryExpression c = constraints.iterator().next();
		assertEquals(ComparisonGt.INSTANCE, c.getOperator());
		assertEquals(y, c.getLeft());
		assertEquals(x, c.getRight());
	}

	@Test
	void constraintsWithLowerBoundReturnsConstraint() throws SemanticException {
		// y < x is in state (x ∈ y's bounds); querying for y should produce y <
		// x
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(y, x);
		// query for y: state has y → {x}, and x is queried as id
		// Loop 2: entry (y → {x}) contains y? No.
		// Loop 1: y's bounds = {x} → add x > y
		// Now query for x itself: x is not in any bounds, no constraints from
		// loop 1
		// BUT for loop 2: entry (y → {x}) contains x → add y < x
		Set<BinaryExpression> constraintsForX = domain.constraints(null, state, x, pp, oracle);
		assertNotNull(constraintsForX);
		// x has no direct upper bounds, but y < x (from loop 2: y's value
		// contains x)
		assertEquals(1, constraintsForX.size());
		BinaryExpression c = constraintsForX.iterator().next();
		assertEquals(ComparisonLt.INSTANCE, c.getOperator());
		assertEquals(y, c.getLeft());
		assertEquals(x, c.getRight());
	}

	@Test
	void constraintsForVariableWithNoBoundsReturnsEmpty() throws SemanticException {
		// state tracks x → {y}, but we query for z (which has no bounds info)
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		Set<BinaryExpression> constraints = domain.constraints(null, state, z, pp, oracle);
		assertNotNull(constraints);
		assertTrue(constraints.isEmpty());
	}

	@Test
	void constraintsForNonIdentifierExpressionReturnsEmpty() throws SemanticException {
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, mkConst(5), ComparisonGt.INSTANCE, pp.getLocation());
		Set<BinaryExpression> constraints = domain.constraints(null, state, expr, pp, oracle);
		assertNotNull(constraints);
		// satisfies(state, x > 5) = UNKNOWN → empty
		assertTrue(constraints.isEmpty());
	}

	@Test
	void constraintsForSatisfiedComparisonExpressionReturnsEquality() throws SemanticException {
		// x < y (x's bounds contain y), and we ask for constraints on
		// BinaryExpr "x < y"
		// satisfies returns SATISFIED → makeEqConstraint(true, expr)
		ValueEnvironment<DefiniteIdSet> state = stateWithBound(x, y);
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		Set<BinaryExpression> constraints = domain.constraints(null, state, expr, pp, oracle);
		assertNotNull(constraints);
		// Should return a boolean equality constraint saying expr == true
		assertEquals(1, constraints.size());
		BinaryExpression c = constraints.iterator().next();
		assertEquals(ComparisonEq.INSTANCE, c.getOperator());
	}
}
