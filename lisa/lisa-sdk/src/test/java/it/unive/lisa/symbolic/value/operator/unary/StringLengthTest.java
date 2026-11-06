package it.unive.lisa.symbolic.value.operator.unary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringLengthTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	@Test
	public void isAStringOperatorAndPrintsStrlen() {
		assertTrue(StringLength.INSTANCE instanceof StringOperator);
		assertEquals("strlen", StringLength.INSTANCE.toString());
	}

	@Test
	public void typeInferenceConvertsStringArgumentToTheIntegerType() {
		Set<Type> mixed = new HashSet<>(Arrays.asList(FixtureTypeSystem.STR, FixtureTypeSystem.BOOL));
		assertEquals(Collections.singleton(FixtureTypeSystem.INT), StringLength.INSTANCE.typeInference(TS, mixed));
	}

	@Test
	public void typeInferenceIsEmptyWhenArgumentHasNoStringType() {
		Set<Type> nonString = new HashSet<>(Arrays.asList(FixtureTypeSystem.INT, FixtureTypeSystem.BOOL));
		assertTrue(StringLength.INSTANCE.typeInference(TS, nonString).isEmpty());
	}

}
