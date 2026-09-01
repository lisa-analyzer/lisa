package it.unive.lisa.symbolic.value.operator.unary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.TypeOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TypeOfTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	@Test
	public void isTypeOperatorAndPrintsTypeof() {
		assertTrue(TypeOf.INSTANCE instanceof TypeOperator);
		assertEquals("typeof", TypeOf.INSTANCE.toString());
	}

	@Test
	public void typeInferenceAcceptsAnyTypeAndWrapsAllOfThemInAToken() {
		// unlike every other unary operator, TypeOf does not filter its
		// argument by any marker interface: it applies uniformly to any Type
		Set<Type> mixed = new HashSet<>(
				Arrays.asList(FixtureTypeSystem.INT, FixtureTypeSystem.STR, FixtureTypeSystem.OTHER));
		Set<Type> result = TypeOf.INSTANCE.typeInference(TS, mixed);
		assertEquals(1, result.size());
		TypeTokenType token = (TypeTokenType) result.iterator().next();
		assertEquals(mixed, token.getTypes());
	}

	@Test
	public void typeInferenceOnAnEmptyArgumentYieldsAnEmptyToken() {
		Set<Type> result = TypeOf.INSTANCE.typeInference(TS, Collections.emptySet());
		assertEquals(1, result.size());
		TypeTokenType token = (TypeTokenType) result.iterator().next();
		assertTrue(token.getTypes().isEmpty());
	}

}
