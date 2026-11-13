package it.unive.lisa.lattices.informationFlow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class NonInterferenceValueTest {

	@Test
	public void constantsHaveTheExpectedConfidentialityAndIntegrity() {
		assertTrue(NonInterferenceValue.LOW_HIGH.isLowConfidentiality());
		assertTrue(NonInterferenceValue.LOW_HIGH.isHighIntegrity());

		assertTrue(NonInterferenceValue.LOW_LOW.isLowConfidentiality());
		assertTrue(NonInterferenceValue.LOW_LOW.isLowIntegrity());

		assertTrue(NonInterferenceValue.HIGH_HIGH.isHighConfidentiality());
		assertTrue(NonInterferenceValue.HIGH_HIGH.isHighIntegrity());

		assertTrue(NonInterferenceValue.HIGH_LOW.isHighConfidentiality());
		assertTrue(NonInterferenceValue.HIGH_LOW.isLowIntegrity());
	}

	@Test
	public void topAndBottom() {
		assertEquals(NonInterferenceValue.HIGH_LOW, NonInterferenceValue.LOW_HIGH.top());
		assertEquals(NonInterferenceValue.BOTTOM, NonInterferenceValue.LOW_HIGH.bottom());
	}

	@Test
	public void lowHighIsTheBottomNonBottomElement()
			throws SemanticException {
		assertTrue(NonInterferenceValue.LOW_HIGH.lessOrEqual(NonInterferenceValue.HIGH_HIGH));
		assertTrue(NonInterferenceValue.LOW_HIGH.lessOrEqual(NonInterferenceValue.LOW_LOW));
		assertTrue(NonInterferenceValue.LOW_HIGH.lessOrEqual(NonInterferenceValue.HIGH_LOW));
		assertTrue(NonInterferenceValue.BOTTOM.lessOrEqual(NonInterferenceValue.LOW_HIGH));
	}

	@Test
	public void highLowIsTheTopElement()
			throws SemanticException {
		assertTrue(NonInterferenceValue.HIGH_HIGH.lessOrEqual(NonInterferenceValue.HIGH_LOW));
		assertTrue(NonInterferenceValue.LOW_LOW.lessOrEqual(NonInterferenceValue.HIGH_LOW));
	}

	@Test
	public void highHighAndLowLowAreIncomparable()
			throws SemanticException {
		assertFalse(NonInterferenceValue.HIGH_HIGH.lessOrEqual(NonInterferenceValue.LOW_LOW));
		assertFalse(NonInterferenceValue.LOW_LOW.lessOrEqual(NonInterferenceValue.HIGH_HIGH));
	}

	@Test
	public void lubOfIncomparableMiddleElementsIsTop()
			throws SemanticException {
		assertEquals(NonInterferenceValue.HIGH_LOW, NonInterferenceValue.HIGH_HIGH.lub(NonInterferenceValue.LOW_LOW));
		assertEquals(NonInterferenceValue.HIGH_LOW, NonInterferenceValue.LOW_LOW.lub(NonInterferenceValue.HIGH_HIGH));
	}

	@Test
	public void lubOfBottomElementIsTheOtherOperand()
			throws SemanticException {
		assertEquals(NonInterferenceValue.HIGH_HIGH, NonInterferenceValue.LOW_HIGH.lub(NonInterferenceValue.HIGH_HIGH));
	}

	@Test
	public void reflexiveLessOrEqual()
			throws SemanticException {
		for (NonInterferenceValue v : new NonInterferenceValue[] {
				NonInterferenceValue.LOW_HIGH,
				NonInterferenceValue.LOW_LOW,
				NonInterferenceValue.HIGH_HIGH,
				NonInterferenceValue.HIGH_LOW })
			assertTrue(v.lessOrEqual(v));
	}

}
