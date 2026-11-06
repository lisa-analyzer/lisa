package it.unive.lisa.symbolic.value.operator.unary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

// StringReverse/ToLowerCase/ToUpperCase/Trim: string in, string out
public class StringUnaryOperatorsTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	private static final Map<UnaryOperator, String> OPERATORS = new LinkedHashMap<>();
	static {
		OPERATORS.put(StringReverse.INSTANCE, "strreverse");
		OPERATORS.put(StringToLowerCase.INSTANCE, "strtolower");
		OPERATORS.put(StringToUpperCase.INSTANCE, "strtoupper");
		OPERATORS.put(StringTrim.INSTANCE, "strtrim");
	}

	@Test
	public void isAStringOperatorAndToStringMatches() {
		for (Map.Entry<UnaryOperator, String> entry : OPERATORS.entrySet()) {
			assertTrue(entry.getKey() instanceof StringOperator, entry.getKey().getClass().getSimpleName());
			assertEquals(entry.getValue(), entry.getKey().toString(), entry.getKey().getClass().getSimpleName());
		}
	}

	@Test
	public void typeInferenceOnAStringArgumentYieldsString() {
		Set<Type> mixed = new HashSet<>(Arrays.asList(FixtureTypeSystem.STR, FixtureTypeSystem.INT));
		for (UnaryOperator op : OPERATORS.keySet())
			assertEquals(
					Collections.singleton(FixtureTypeSystem.STR),
					op.typeInference(TS, mixed),
					op.getClass().getSimpleName());
	}

	@Test
	public void typeInferenceIsEmptyWhenArgumentHasNoStringType() {
		Set<Type> nonString = new HashSet<>(Arrays.asList(FixtureTypeSystem.INT, FixtureTypeSystem.BOOL));
		for (UnaryOperator op : OPERATORS.keySet())
			assertTrue(op.typeInference(TS, nonString).isEmpty(), op.getClass().getSimpleName());
	}

}
