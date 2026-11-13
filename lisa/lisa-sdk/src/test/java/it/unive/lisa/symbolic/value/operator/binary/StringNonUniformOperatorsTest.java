package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.CHAR;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.INT32;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

// covers the binary string operators that do NOT extend StringOperation
// because their second operand is not itself a string (a character or an
// integer index), so each hand-rolls its own typeInference
public class StringNonUniformOperatorsTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	@Test
	public void stringCharAtTakesAStringAndANumberAndYieldsACharacter() {
		assertEquals("strcharat", StringCharAt.INSTANCE.toString());
		Set<Type> strs = Collections.singleton(STR);
		Set<Type> nums = Collections.singleton(INT32);
		assertEquals(Collections.singleton(CHAR), StringCharAt.INSTANCE.typeInference(TS, strs, nums));
		assertTrue(StringCharAt.INSTANCE.typeInference(TS, nums, nums).isEmpty(), "left operand must be a string");
		assertTrue(StringCharAt.INSTANCE.typeInference(TS, strs, strs).isEmpty(), "right operand must be numeric");
	}

	@Test
	public void stringIndexOfCharTakesAStringAndACharacterAndYieldsAnInteger() {
		assertEquals("strindexofchar", StringIndexOfChar.INSTANCE.toString());
		Set<Type> strs = Collections.singleton(STR);
		Set<Type> chars = Collections.singleton(CHAR);
		assertEquals(Collections.singleton(INT32), StringIndexOfChar.INSTANCE.typeInference(TS, strs, chars));
		assertTrue(StringIndexOfChar.INSTANCE.typeInference(TS, chars, chars).isEmpty(),
				"left operand must be a string");
		assertTrue(StringIndexOfChar.INSTANCE.typeInference(TS, strs, strs).isEmpty(),
				"right operand must be a character");
	}

	@Test
	public void stringLastIndexOfCharTakesAStringAndACharacterAndYieldsAnInteger() {
		assertEquals("strlastindexofchar", StringLastIndexOfChar.INSTANCE.toString());
		Set<Type> strs = Collections.singleton(STR);
		Set<Type> chars = Collections.singleton(CHAR);
		assertEquals(Collections.singleton(INT32), StringLastIndexOfChar.INSTANCE.typeInference(TS, strs, chars));
		assertTrue(StringLastIndexOfChar.INSTANCE.typeInference(TS, chars, chars).isEmpty(),
				"left operand must be a string");
		assertTrue(StringLastIndexOfChar.INSTANCE.typeInference(TS, strs, strs).isEmpty(),
				"right operand must be a character");
	}

	@Test
	public void stringSubstringToEndTakesAStringAndANumberAndYieldsAString() {
		assertEquals("strsub", StringSubstringToEnd.INSTANCE.toString());
		Set<Type> strs = Collections.singleton(STR);
		Set<Type> nums = Collections.singleton(INT32);
		assertEquals(Collections.singleton(STR), StringSubstringToEnd.INSTANCE.typeInference(TS, strs, nums));
		assertTrue(StringSubstringToEnd.INSTANCE.typeInference(TS, nums, nums).isEmpty(),
				"left operand must be a string");
		assertTrue(StringSubstringToEnd.INSTANCE.typeInference(TS, strs, strs).isEmpty(),
				"right operand must be numeric");
	}

}
