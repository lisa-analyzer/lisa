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
import it.unive.lisa.lattices.numeric.PentagonLattice;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingRem;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class PentagonTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final Pentagon domain = new Pentagon();
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final Variable x = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable y = new Variable(Int32Type.INSTANCE, "y", pp.getLocation());
	private final Variable r = new Variable(Int32Type.INSTANCE, "r", pp.getLocation());
	private final Variable d = new Variable(Int32Type.INSTANCE, "d", pp.getLocation());
	private final Variable u = new Variable(Int32Type.INSTANCE, "u", pp.getLocation());

	private Constant mkConst(
			int v) {
		return new Constant(Int32Type.INSTANCE, v, pp.getLocation());
	}

	/** Build a state with specific interval values and no upper-bound info. */
	private PentagonLattice stateWithIntervals(
			Variable v1,
			IntInterval i1,
			Variable v2,
			IntInterval i2)
			throws SemanticException {
		ValueEnvironment<IntInterval> intv = new Interval().makeLattice()
				.putState(v1, i1)
				.putState(v2, i2);
		ValueEnvironment<DefiniteIdSet> ub = new UpperBounds().makeLattice();
		return new PentagonLattice(intv, ub);
	}

	/**
	 * Build a state with interval values plus one upper-bound entry (a < b).
	 */
	private PentagonLattice stateWithIntervalsAndBound(
			Variable v1,
			IntInterval i1,
			Variable v2,
			IntInterval i2,
			Variable small,
			Variable big)
			throws SemanticException {
		// small < big → big ∈ small's upper-bound set
		ValueEnvironment<IntInterval> intv = new Interval().makeLattice()
				.putState(v1, i1)
				.putState(v2, i2);
		ValueEnvironment<DefiniteIdSet> ub = new UpperBounds().makeLattice()
				.putState(small, new DefiniteIdSet(Set.of(big)));
		return new PentagonLattice(intv, ub);
	}

	// --- assign ---

	@Test
	void assignConstantSetsInterval() throws SemanticException {
		PentagonLattice state = domain.makeLattice();
		PentagonLattice result = domain.assign(state, x, mkConst(7), pp, oracle);
		assertEquals(new IntInterval(7, 7), result.first.getState(x));
	}

	@Test
	void assignSubtractionWithKnownPositiveDivisorRefinesInterval() throws SemanticException {
		// x ∈ [3,10], y ∈ [1,5], and y < x (y's upper bound set contains x)
		// r = x - y → interval is [-2,9] but reduced to [1,9] because y < x
		PentagonLattice state = stateWithIntervalsAndBound(
				x, new IntInterval(3, 10),
				y, new IntInterval(1, 5),
				y, x); // y < x
		BinaryExpression subExpr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, NumericNonOverflowingSub.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assign(state, r, subExpr, pp, oracle);
		// r must be ≥ 1 because y < x implies x - y ≥ 1
		assertEquals(new IntInterval(1, 9), result.first.getState(r));
	}

	@Test
	void assignSubtractionWithoutBoundGivesRawInterval() throws SemanticException {
		// x ∈ [3,10], y ∈ [1,5], no upper-bound relation → r = x - y = [-2,9]
		PentagonLattice state = stateWithIntervals(x, new IntInterval(3, 10), y, new IntInterval(1, 5));
		BinaryExpression subExpr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, NumericNonOverflowingSub.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assign(state, r, subExpr, pp, oracle);
		assertEquals(new IntInterval(-2, 9), result.first.getState(r));
	}

	@Test
	void assignSubtractionWithPositiveDivisorAddsUpperBound() throws SemanticException {
		// y ∈ [2,5] (low > 0) and y < x → r = x - y, r's upper bounds include x
		PentagonLattice state = stateWithIntervalsAndBound(
				x, new IntInterval(5, 10),
				y, new IntInterval(2, 3),
				y, x); // y < x
		BinaryExpression subExpr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, NumericNonOverflowingSub.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assign(state, r, subExpr, pp, oracle);
		// r < x because r = x - y where y > 0
		assertTrue(result.second.getState(r).contains(x));
	}

	@Test
	void assignRemainderWithNonNegativeDivisorAddsUpperBound() throws SemanticException {
		// d ∈ [3,7] (low > 0), r = u % d → d is an upper bound of r
		PentagonLattice state = stateWithIntervals(u, new IntInterval(0, 100), d, new IntInterval(3, 7));
		BinaryExpression remExpr = new BinaryExpression(
				Int32Type.INSTANCE, u, d, NumericNonOverflowingRem.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assign(state, r, remExpr, pp, oracle);
		// r < d because 0 <= (u % d) < d for non-negative d
		assertTrue(result.second.getState(r).contains(d));
	}

	@Test
	void assignRemainderWithNegativeDivisorDoesNotAddUpperBound() throws SemanticException {
		// d ∈ [-5,-1] (low < 0) → upper-bound guarantee does not hold
		PentagonLattice state = stateWithIntervals(u, new IntInterval(0, 100), d, new IntInterval(-5, -1));
		BinaryExpression remExpr = new BinaryExpression(
				Int32Type.INSTANCE, u, d, NumericNonOverflowingRem.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assign(state, r, remExpr, pp, oracle);
		assertTrue(result.second.getState(r).isTop());
	}

	// --- satisfies ---

	@Test
	void satisfiesGtFromBothComponents() throws SemanticException {
		// x ∈ [6,10], y ∈ [1,5] → interval says x > y (SATISFIED)
		// y's upper-bound set contains x → y < x → x > y (SATISFIED)
		// glb = SATISFIED
		PentagonLattice state = stateWithIntervalsAndBound(
				x, new IntInterval(6, 10),
				y, new IntInterval(1, 5),
				y, x); // y < x
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesGtFromIntervalAloneGivesSatisfied() throws SemanticException {
		// x ∈ [6,10], y ∈ [1,5] → intervals say SATISFIED, upper bounds say
		// UNKNOWN
		// UNKNOWN is TOP so glb(SATISFIED, UNKNOWN) = SATISFIED
		PentagonLattice state = stateWithIntervals(x, new IntInterval(6, 10), y, new IntInterval(1, 5));
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED, domain.satisfies(state, expr, pp, oracle));
	}

	@Test
	void satisfiesLtFromUpperBoundsAloneGivesSatisfied() throws SemanticException {
		// Overlapping intervals → intervals say UNKNOWN; x < y from upper
		// bounds → SATISFIED
		// UNKNOWN is TOP so glb(UNKNOWN, SATISFIED) = SATISFIED
		PentagonLattice state = stateWithIntervalsAndBound(
				x, new IntInterval(1, 10),
				y, new IntInterval(1, 10),
				x, y); // x < y
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.SATISFIED, domain.satisfies(state, expr, pp, oracle));
	}

	// --- assume ---

	@Test
	void assumeEqRefinesIntervalComponent() throws SemanticException {
		PentagonLattice state = stateWithIntervals(x, new IntInterval(1, 10), y, new IntInterval(1, 10));
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, mkConst(5), ComparisonEq.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assume(state, cond, pp, pp, oracle);
		assertEquals(new IntInterval(5, 5), result.first.getState(x));
	}

	@Test
	void assumeLtBetweenIdentifiersRefinesUpperBounds() throws SemanticException {
		PentagonLattice state = stateWithIntervals(x, new IntInterval(1, 10), y, new IntInterval(1, 10));
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonLt.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assume(state, cond, pp, pp, oracle);
		// After x < y, y should be in x's upper-bound set
		assertTrue(result.second.getState(x).contains(y));
	}

	@Test
	void assumeOnBottomReturnsBottom() throws SemanticException {
		PentagonLattice bottom = domain.makeLattice().bottom();
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, mkConst(5), ComparisonGt.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assume(bottom, cond, pp, pp, oracle);
		assertTrue(result.isBottom());
	}

	// --- constraints ---

	@Test
	void constraintsOnBottomReturnsNull() throws SemanticException {
		PentagonLattice bottom = domain.makeLattice().bottom();
		assertNull(domain.constraints(null, bottom, x, pp, oracle));
	}

	@Test
	void constraintsOnTopReturnsEmpty() throws SemanticException {
		PentagonLattice top = domain.makeLattice();
		Set<BinaryExpression> constraints = domain.constraints(null, top, x, pp, oracle);
		assertNotNull(constraints);
		assertTrue(constraints.isEmpty());
	}

	@Test
	void constraintsOnBoundedIntervalReturnsRangeConstraints() throws SemanticException {
		// x ∈ [2, 8] → two range constraints from interval component
		PentagonLattice state = stateWithIntervals(x, new IntInterval(2, 8), y, IntInterval.TOP);
		Set<BinaryExpression> constraints = domain.constraints(null, state, x, pp, oracle);
		assertNotNull(constraints);
		// At least lower + upper bound from interval
		assertEquals(2, constraints.size());
	}

	@Test
	void constraintsBothComponentsNullReturnsNull() throws SemanticException {
		// Bottom state → both components return null → union is null
		PentagonLattice bottom = domain.makeLattice().bottom();
		assertNull(domain.constraints(null, bottom, x, pp, oracle));
	}

	@Test
	void constraintsIntervalBoundedUpperBoundsTopReturnsIntervalConstraints() throws SemanticException {
		// Intervals are bounded, UpperBounds is TOP (empty constraints)
		// → result = interval constraints (non-empty)
		ValueEnvironment<IntInterval> intv = new Interval().makeLattice()
				.putState(x, new IntInterval(1, 5));
		ValueEnvironment<DefiniteIdSet> ub = new UpperBounds().makeLattice();
		PentagonLattice state = new PentagonLattice(intv, ub);
		Set<BinaryExpression> constraints = domain.constraints(null, state, x, pp, oracle);
		assertNotNull(constraints);
		assertEquals(2, constraints.size());
	}

	@Test
	void constraintsCombinesBothComponentsForIdentifier() throws SemanticException {
		// x ∈ [2,8] (2 range constraints) AND y ∈ x's upper bounds (1
		// upper-bound constraint)
		// → total 3 constraints
		ValueEnvironment<IntInterval> intv = new Interval().makeLattice()
				.putState(x, new IntInterval(2, 8));
		ValueEnvironment<DefiniteIdSet> ub = new UpperBounds().makeLattice()
				.putState(x, new DefiniteIdSet(Set.of(y)));
		PentagonLattice state = new PentagonLattice(intv, ub);
		Set<BinaryExpression> constraints = domain.constraints(null, state, x, pp, oracle);
		assertNotNull(constraints);
		assertEquals(3, constraints.size());
	}

	// --- satisfies: UNKNOWN case ---

	@Test
	void satisfiesGtWithOverlappingIntervalsAndNoUpperBoundGivesUnknown() throws SemanticException {
		// x ∈ [1,10], y ∈ [1,10] → intervals say UNKNOWN; no bound info →
		// UNKNOWN
		PentagonLattice state = stateWithIntervals(x, new IntInterval(1, 10), y, new IntInterval(1, 10));
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGt.INSTANCE, pp.getLocation());
		assertEquals(Satisfiability.UNKNOWN, domain.satisfies(state, expr, pp, oracle));
	}

	// --- assume: additional operators ---

	@Test
	void assumeGtBetweenIdentifiersRefinesUpperBoundsOfRightOperand() throws SemanticException {
		// assume x > y → y < x, so x should be in y's upper-bound set
		PentagonLattice state = stateWithIntervals(x, new IntInterval(1, 10), y, new IntInterval(1, 10));
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, y, ComparisonGt.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assume(state, cond, pp, pp, oracle);
		assertTrue(result.second.getState(y).contains(x));
	}

	@Test
	void assumeGtWithConstantRefinesIntervalComponent() throws SemanticException {
		// x ∈ [1,10], assume x > 5 → x ∈ [6,10]
		PentagonLattice state = stateWithIntervals(x, new IntInterval(1, 10), y, IntInterval.TOP);
		BinaryExpression cond = new BinaryExpression(
				Int32Type.INSTANCE, x, mkConst(5), ComparisonGt.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.assume(state, cond, pp, pp, oracle);
		assertEquals(new IntInterval(6, 10), result.first.getState(x));
	}

	// --- smallStepSemantics ---

	@Test
	void smallStepSemanticsLeavesStateUnchanged() throws SemanticException {
		// smallStepSemantics just delegates to both components without
		// modifying state
		PentagonLattice state = stateWithIntervals(x, new IntInterval(1, 5), y, new IntInterval(7, 10));
		BinaryExpression expr = new BinaryExpression(
				Int32Type.INSTANCE, x, y, NumericNonOverflowingAdd.INSTANCE, pp.getLocation());
		PentagonLattice result = domain.smallStepSemantics(state, expr, pp, oracle);
		// The result should equal the input state (smallStepSemantics only
		// records the expression)
		assertEquals(state, result);
	}

	// --- canProcess ---

	@Test
	void canProcessReturnsTrueForNumericVariable() {
		assertTrue(domain.canProcess(x, pp, oracle));
	}

	// --- closure: intervals trigger upper-bound inference ---

	@Test
	void assignTriggersClosureLinkingIntervalAndUpperBounds() throws SemanticException {
		// x ∈ [1,3], y ∈ [5,10]; after any assign, closure finds x.high=3 <
		// y.low=5
		// and adds y to x's upper-bound set
		PentagonLattice state = stateWithIntervals(x, new IntInterval(1, 3), y, new IntInterval(5, 10));
		// Assign r = 0 (a neutral operation that triggers closure without
		// changing x/y)
		PentagonLattice result = domain.assign(state, r, mkConst(0), pp, oracle);
		assertTrue(result.second.getState(x).contains(y));
	}

	@Test
	public void testBottomIntervalClosure() throws SemanticException {

		Identifier varX = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
		Identifier varY = new Variable(Int32Type.INSTANCE, "y", pp.getLocation());

		Map<Identifier, IntInterval> intervalFunction = new HashMap<>();
		intervalFunction.put(varX, IntInterval.ONE);
		intervalFunction.put(varY, IntInterval.BOTTOM);
		ValueEnvironment<IntInterval> interval = new ValueEnvironment<IntInterval>(IntInterval.TOP, intervalFunction);

		Map<Identifier, DefiniteIdSet> boundsFunction = new HashMap<>();

		boundsFunction.put(varX, new DefiniteIdSet(new HashSet<>()));
		boundsFunction.put(varY, new DefiniteIdSet(Set.of(varX)));

		ValueEnvironment<DefiniteIdSet> bounds = new ValueEnvironment<DefiniteIdSet>(
				new DefiniteIdSet(new HashSet<Identifier>()), boundsFunction);

		PentagonLattice pentagonLattice = new PentagonLattice(interval, bounds);
		pentagonLattice.closure();
	}
}
