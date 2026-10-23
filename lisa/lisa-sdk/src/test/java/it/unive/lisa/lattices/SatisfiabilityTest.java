package it.unive.lisa.lattices;

import static it.unive.lisa.lattices.Satisfiability.BOTTOM;
import static it.unive.lisa.lattices.Satisfiability.NOT_SATISFIED;
import static it.unive.lisa.lattices.Satisfiability.SATISFIED;
import static it.unive.lisa.lattices.Satisfiability.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class SatisfiabilityTest {

	@Test
	public void topIsUnknownAndBottomIsError() {
		assertSame(UNKNOWN, SATISFIED.top());
		assertSame(BOTTOM, SATISFIED.bottom());
	}

	@Test
	public void fromBooleanMapsTrueAndFalse() {
		assertSame(SATISFIED, Satisfiability.fromBoolean(true));
		assertSame(NOT_SATISFIED, Satisfiability.fromBoolean(false));
	}

	@Test
	public void negateFlipsSatisfiedAndNotSatisfiedButLeavesUnknownAndBottomUnchanged() {
		assertSame(NOT_SATISFIED, SATISFIED.negate());
		assertSame(SATISFIED, NOT_SATISFIED.negate());
		assertSame(UNKNOWN, UNKNOWN.negate());
		assertSame(BOTTOM, BOTTOM.negate());
	}

	@Test
	public void mightBeTrueAndMightBeFalseReflectTheThreeValuedSemantics() {
		assertTrue(SATISFIED.mightBeTrue());
		assertFalse(SATISFIED.mightBeFalse());
		assertTrue(NOT_SATISFIED.mightBeFalse());
		assertFalse(NOT_SATISFIED.mightBeTrue());
		assertTrue(UNKNOWN.mightBeTrue());
		assertTrue(UNKNOWN.mightBeFalse());
		assertFalse(BOTTOM.mightBeTrue());
		assertFalse(BOTTOM.mightBeFalse());
	}

	@Test
	public void andImplementsThreeValuedLogic() {
		assertSame(NOT_SATISFIED, SATISFIED.and(NOT_SATISFIED));
		assertSame(SATISFIED, SATISFIED.and(SATISFIED));
		assertSame(UNKNOWN, SATISFIED.and(UNKNOWN));

		assertSame(NOT_SATISFIED, NOT_SATISFIED.and(SATISFIED));
		assertSame(NOT_SATISFIED, NOT_SATISFIED.and(UNKNOWN));

		assertSame(NOT_SATISFIED, UNKNOWN.and(NOT_SATISFIED));
		assertSame(UNKNOWN, UNKNOWN.and(SATISFIED));
		assertSame(UNKNOWN, UNKNOWN.and(UNKNOWN));

		assertSame(BOTTOM, BOTTOM.and(SATISFIED));
	}

	@Test
	public void orImplementsThreeValuedLogic() {
		assertSame(SATISFIED, SATISFIED.or(NOT_SATISFIED));
		assertSame(SATISFIED, SATISFIED.or(UNKNOWN));

		assertSame(SATISFIED, NOT_SATISFIED.or(SATISFIED));
		assertSame(UNKNOWN, NOT_SATISFIED.or(UNKNOWN));
		assertSame(NOT_SATISFIED, NOT_SATISFIED.or(NOT_SATISFIED));

		assertSame(SATISFIED, UNKNOWN.or(SATISFIED));
		assertSame(UNKNOWN, UNKNOWN.or(NOT_SATISFIED));
		assertSame(UNKNOWN, UNKNOWN.or(UNKNOWN));

		assertSame(BOTTOM, BOTTOM.or(SATISFIED));
	}

	@Test
	public void satisfiedAndNotSatisfiedAreIncomparableAndJoinToUnknown() throws SemanticException {
		// SATISFIED/NOT_SATISFIED are the only two "middle" elements of this
		// diamond lattice, and they are incomparable
		assertFalse(SATISFIED.lessOrEqual(NOT_SATISFIED));
		assertFalse(NOT_SATISFIED.lessOrEqual(SATISFIED));
		assertSame(UNKNOWN, SATISFIED.lub(NOT_SATISFIED));
		assertSame(BOTTOM, SATISFIED.glb(NOT_SATISFIED));
	}

	@Test
	public void lubAndGlbWithTopAndBottomFollowTheLatticeShortCircuits() throws SemanticException {
		assertSame(UNKNOWN, SATISFIED.lub(UNKNOWN));
		assertSame(SATISFIED, SATISFIED.lub(BOTTOM));
		assertSame(SATISFIED, SATISFIED.glb(UNKNOWN));
		assertSame(BOTTOM, SATISFIED.glb(BOTTOM));
	}

	@Test
	public void toStringIsTheEnumName() {
		assertEquals("SATISFIED", SATISFIED.representation().toString());
	}

}
