package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.BOOL;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.INT32;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.STR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ValueComparisonTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	@Test
	public void toStringIsCompareTo() {
		assertEquals("compareTo", ValueComparison.INSTANCE.toString());
	}

	@Test
	public void typeInferenceAlwaysYieldsTheIntegerTypeRegardlessOfOperandTypes() {
		// ValueComparison is a three-way, compareTo-style operator (see its
		// javadoc: "if equal the result is 0; if smaller, negative;
		// otherwise positive"), not an equality check - the result is always
		// the type system's integer type, and applies to any pair of types
		Set<Type> expected = Collections.singleton(INT32);
		assertEquals(expected,
				ValueComparison.INSTANCE.typeInference(TS, Collections.singleton(STR), Collections.singleton(BOOL)));
		assertEquals(expected,
				ValueComparison.INSTANCE.typeInference(TS, Collections.emptySet(), Collections.emptySet()));
		assertEquals(expected,
				ValueComparison.INSTANCE.typeInference(TS, Collections.singleton(INT32), Collections.singleton(INT32)));
	}

}
