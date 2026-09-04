package it.unive.lisa.lattices.informationFlow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class ThreeTaintTest {

	@Test
	public void taintedAndCleanAreFixedRegardlessOfInstance() {
		assertEquals(ThreeTaint.TAINTED, ThreeTaint.CLEAN.tainted());
		assertEquals(ThreeTaint.CLEAN, ThreeTaint.TAINTED.clean());
	}

	@Test
	public void topAndBottom() {
		assertEquals(ThreeTaint.TOP, ThreeTaint.CLEAN.top());
		assertEquals(ThreeTaint.BOTTOM, ThreeTaint.CLEAN.bottom());
	}

	@Test
	public void alwaysTaintedOnlyForTainted() {
		assertTrue(ThreeTaint.TAINTED.isAlwaysTainted());
		assertFalse(ThreeTaint.CLEAN.isAlwaysTainted());
		assertFalse(ThreeTaint.TOP.isAlwaysTainted());
		assertFalse(ThreeTaint.BOTTOM.isAlwaysTainted());
	}

	// TaintLattice#isPossiblyTainted() is documented as "definitely tainted in
	// at least one execution path". A value that is tainted on EVERY path
	// (TAINTED) trivially satisfies "tainted on at least one path" too, so
	// TAINTED.isPossiblyTainted() must be true. The current implementation
	// only returns true for TOP, which also makes the default
	// isAlwaysClean() = !isPossiblyTainted() && !isBottom() wrongly report
	// TAINTED.isAlwaysClean() == true: a value that is always tainted is
	// reported as always clean. This looks like a real bug in
	// ThreeTaint#isPossiblyTainted().
	@Test
	public void possiblyTaintedMustHoldForAlwaysTaintedTooPerDocumentedContract() {
		assertTrue(ThreeTaint.TOP.isPossiblyTainted());
		assertTrue(ThreeTaint.TAINTED.isPossiblyTainted());
		assertFalse(ThreeTaint.CLEAN.isPossiblyTainted());
		assertFalse(ThreeTaint.TAINTED.isAlwaysClean());
	}

	@Test
	public void cleanAndTaintedAreIncomparable()
			throws SemanticException {
		assertFalse(ThreeTaint.CLEAN.lessOrEqual(ThreeTaint.TAINTED));
		assertFalse(ThreeTaint.TAINTED.lessOrEqual(ThreeTaint.CLEAN));
	}

	@Test
	public void topIsAboveEverythingAndBottomBelowEverything()
			throws SemanticException {
		assertTrue(ThreeTaint.CLEAN.lessOrEqual(ThreeTaint.TOP));
		assertTrue(ThreeTaint.TAINTED.lessOrEqual(ThreeTaint.TOP));
		assertTrue(ThreeTaint.BOTTOM.lessOrEqual(ThreeTaint.CLEAN));
		assertTrue(ThreeTaint.BOTTOM.lessOrEqual(ThreeTaint.TAINTED));
	}

	@Test
	public void lubOfCleanAndTaintedIsTop()
			throws SemanticException {
		assertEquals(ThreeTaint.TOP, ThreeTaint.CLEAN.lub(ThreeTaint.TAINTED));
		assertEquals(ThreeTaint.TOP, ThreeTaint.TAINTED.lub(ThreeTaint.CLEAN));
	}

	@Test
	public void wideningOfCleanAndTaintedIsTop()
			throws SemanticException {
		assertEquals(ThreeTaint.TOP, ThreeTaint.CLEAN.widening(ThreeTaint.TAINTED));
	}

	// or() combines the taintedness of two sub-expressions: if one operand is
	// tainted on EVERY path (TAINTED), the combined expression is tainted on
	// every path too, regardless of what the other operand does - so TAINTED
	// correctly dominates over TOP (merely "possibly tainted")
	@Test
	public void orGivesTaintedPriorityOverPossiblyTainted()
			throws SemanticException {
		assertEquals(ThreeTaint.TAINTED, ThreeTaint.TAINTED.or(ThreeTaint.TOP));
		assertEquals(ThreeTaint.TAINTED, ThreeTaint.TOP.or(ThreeTaint.TAINTED));
		assertEquals(ThreeTaint.TOP, ThreeTaint.TOP.or(ThreeTaint.CLEAN));
		assertEquals(ThreeTaint.CLEAN, ThreeTaint.CLEAN.or(ThreeTaint.CLEAN));
		assertEquals(ThreeTaint.TAINTED, ThreeTaint.CLEAN.or(ThreeTaint.TAINTED));
	}

}
