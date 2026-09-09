package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.BOOL;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.CHAR;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CharacterEqualsTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	@Test
	public void toStringIsChreq() {
		assertEquals("chreq", CharacterEquals.INSTANCE.toString());
	}

	@Test
	public void typeInferenceRequiresCharacterOnBothSides() {
		Set<Type> chars = Collections.singleton(CHAR);
		Set<Type> other = Collections.singleton(STR);

		assertEquals(Collections.singleton(BOOL), CharacterEquals.INSTANCE.typeInference(TS, chars, chars));
		assertTrue(CharacterEquals.INSTANCE.typeInference(TS, chars, other).isEmpty());
		assertTrue(CharacterEquals.INSTANCE.typeInference(TS, other, chars).isEmpty());
	}

}
