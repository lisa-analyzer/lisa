package it.unive.lisa.symbolic.value.operator.unary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NumericToStringTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	@Test
	public void isArithmeticAndPrintsToString() {
		assertTrue(NumericToString.INSTANCE instanceof ArithmeticOperator);
		assertEquals("toString", NumericToString.INSTANCE.toString());
	}

	@Test
	public void typeInferenceConvertsNumericArgumentToStringType() {
		Set<Type> mixed = new HashSet<>(Arrays.asList(FixtureTypeSystem.INT, FixtureTypeSystem.BOOL));
		assertEquals(
				Collections.singleton(FixtureTypeSystem.STR),
				NumericToString.INSTANCE.typeInference(TS, mixed));
	}

	@Test
	public void typeInferenceIsEmptyWhenArgumentHasNoNumericType() {
		Set<Type> nonNumeric = new HashSet<>(Arrays.asList(FixtureTypeSystem.STR, FixtureTypeSystem.BOOL));
		assertTrue(NumericToString.INSTANCE.typeInference(TS, nonNumeric).isEmpty());
	}

}
