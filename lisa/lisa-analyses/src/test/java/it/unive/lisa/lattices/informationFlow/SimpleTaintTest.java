package it.unive.lisa.lattices.informationFlow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class SimpleTaintTest {

	@Test
	public void tintedAndCleanAreFixedRegardlessOfInstance()
			throws SemanticException {
		assertEquals(SimpleTaint.TAINTED, SimpleTaint.CLEAN.tainted());
		assertEquals(SimpleTaint.CLEAN, SimpleTaint.TAINTED.clean());
		assertEquals(SimpleTaint.TAINTED, SimpleTaint.BOTTOM.tainted());
		assertEquals(SimpleTaint.CLEAN, SimpleTaint.BOTTOM.clean());
	}

	@Test
	public void taintedIsTop() {
		assertEquals(SimpleTaint.TAINTED, SimpleTaint.CLEAN.top());
	}

	@Test
	public void bottomIsBottom() {
		assertEquals(SimpleTaint.BOTTOM, SimpleTaint.TAINTED.bottom());
	}

	@Test
	public void possiblyTaintedOnlyForTainted() {
		assertTrue(SimpleTaint.TAINTED.isPossiblyTainted());
		assertFalse(SimpleTaint.CLEAN.isPossiblyTainted());
		assertFalse(SimpleTaint.BOTTOM.isPossiblyTainted());
	}

	// a 2-level taint domain cannot distinguish "tainted on every path" from
	// "tainted on at least one path": both collapse onto the same TAINTED
	// element, so isAlwaysTainted() can never be proven and must stay false
	@Test
	public void neverProvenAlwaysTaintedByDesign() {
		assertFalse(SimpleTaint.TAINTED.isAlwaysTainted());
		assertFalse(SimpleTaint.CLEAN.isAlwaysTainted());
	}

	@Test
	public void alwaysCleanOnlyForClean() {
		assertTrue(SimpleTaint.CLEAN.isAlwaysClean());
		assertFalse(SimpleTaint.TAINTED.isAlwaysClean());
	}

	@Test
	public void lessOrEqualOrdersCleanBelowTainted()
			throws SemanticException {
		assertTrue(SimpleTaint.CLEAN.lessOrEqual(SimpleTaint.TAINTED));
		assertFalse(SimpleTaint.TAINTED.lessOrEqual(SimpleTaint.CLEAN));
		assertTrue(SimpleTaint.BOTTOM.lessOrEqual(SimpleTaint.CLEAN));
		assertTrue(SimpleTaint.BOTTOM.lessOrEqual(SimpleTaint.TAINTED));
	}

	@Test
	public void orIsTheJoinOfTheTwoOperandsTaintedness()
			throws SemanticException {
		assertEquals(SimpleTaint.CLEAN, SimpleTaint.CLEAN.or(SimpleTaint.CLEAN));
		assertEquals(SimpleTaint.TAINTED, SimpleTaint.CLEAN.or(SimpleTaint.TAINTED));
		assertEquals(SimpleTaint.TAINTED, SimpleTaint.TAINTED.or(SimpleTaint.CLEAN));
		assertEquals(SimpleTaint.TAINTED, SimpleTaint.TAINTED.or(SimpleTaint.TAINTED));
	}

	@Test
	public void equalsDistinguishesTheThreeInstances() {
		assertNotEquals(SimpleTaint.CLEAN, SimpleTaint.TAINTED);
		assertNotEquals(SimpleTaint.CLEAN, SimpleTaint.BOTTOM);
		assertNotEquals(SimpleTaint.TAINTED, SimpleTaint.BOTTOM);
		assertEquals(SimpleTaint.CLEAN.hashCode(), SimpleTaint.CLEAN.hashCode());
	}

}
