package it.unive.lisa.symbolic.value.operator.unary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CharacterCaseConversionTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	private static final Map<UnaryOperator, String> OPERATORS = new LinkedHashMap<>();
	static {
		OPERATORS.put(CharacterToLowerCase.INSTANCE, "toLowercase");
		OPERATORS.put(CharacterToUpperCase.INSTANCE, "toUppercase");
	}

	@Test
	public void toStringMatchesTheDocumentedSymbol() {
		for (Map.Entry<UnaryOperator, String> entry : OPERATORS.entrySet())
			assertEquals(entry.getValue(), entry.getKey().toString(), entry.getKey().getClass().getSimpleName());
	}

	@Test
	public void typeInferenceOnACharacterArgumentYieldsCharacter() {
		Set<Type> mixed = new HashSet<>(Arrays.asList(FixtureTypeSystem.CHAR, FixtureTypeSystem.INT));
		for (UnaryOperator op : OPERATORS.keySet())
			assertEquals(
					Collections.singleton(FixtureTypeSystem.CHAR),
					op.typeInference(TS, mixed),
					op.getClass().getSimpleName());
	}

	@Test
	public void typeInferenceIsEmptyWhenArgumentHasNoCharacterType() {
		Set<Type> nonCharacter = new HashSet<>(Arrays.asList(FixtureTypeSystem.INT, FixtureTypeSystem.STR));
		for (UnaryOperator op : OPERATORS.keySet())
			assertTrue(op.typeInference(TS, nonCharacter).isEmpty(), op.getClass().getSimpleName());
	}

}
