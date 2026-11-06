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

// the 8 Character*Is* predicates all share the exact same typeInference()
// shape (character in, boolean out); as with the numeric passthrough
// operators, each hand-writes its own copy with no shared base
public class CharacterPredicatesTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	private static final Map<UnaryOperator, String> OPERATORS = new LinkedHashMap<>();
	static {
		OPERATORS.put(CharacterIsDefined.INSTANCE, "isDefined");
		OPERATORS.put(CharacterIsDigit.INSTANCE, "isDigit");
		OPERATORS.put(CharacterIsIdentifierPart.INSTANCE, "isIdentifierPart");
		OPERATORS.put(CharacterIsIdentifierStart.INSTANCE, "isIdentifierStart");
		OPERATORS.put(CharacterIsLetter.INSTANCE, "isLetter");
		OPERATORS.put(CharacterIsLetterOrDigit.INSTANCE, "isLetterOrDigit");
		OPERATORS.put(CharacterIsLowerCase.INSTANCE, "isLowercase");
		OPERATORS.put(CharacterIsUpperCase.INSTANCE, "isUppercase");
	}

	@Test
	public void toStringMatchesTheDocumentedSymbol() {
		for (Map.Entry<UnaryOperator, String> entry : OPERATORS.entrySet())
			assertEquals(entry.getValue(), entry.getKey().toString(), entry.getKey().getClass().getSimpleName());
	}

	@Test
	public void typeInferenceOnACharacterArgumentYieldsBoolean() {
		Set<Type> mixed = new HashSet<>(Arrays.asList(FixtureTypeSystem.CHAR, FixtureTypeSystem.INT));
		for (UnaryOperator op : OPERATORS.keySet())
			assertEquals(
					Collections.singleton(FixtureTypeSystem.BOOL),
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
