package it.unive.lisa.symbolic.value.operator.ternary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeOtherType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeTypeSystem;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringIndexOfCharFromIndexTest {

	private final FakeTypeSystem ts = new FakeTypeSystem();
	private final Set<Type> string = Collections.singleton(ts.stringType);
	private final Set<Type> character = Collections.singleton(ts.characterType);
	private final Set<Type> numeric = Collections.singleton(ts.integerType);
	private final Set<Type> other = Collections.singleton(new FakeOtherType());

	@Test
	public void stringCharAndNumberYieldInteger() {
		assertEquals(
				Collections.singleton(ts.integerType),
				StringIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, character, numeric));
	}

	@Test
	public void aNonStringFirstArgumentYieldsEmpty() {
		assertTrue(StringIndexOfCharFromIndex.INSTANCE.typeInference(ts, other, character, numeric).isEmpty());
	}

	@Test
	public void aNonCharacterSecondArgumentYieldsEmpty() {
		// a plain string in the "character" slot must not be accepted
		assertTrue(StringIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, string, numeric).isEmpty());
		assertTrue(StringIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, other, numeric).isEmpty());
	}

	@Test
	public void aNonNumericThirdArgumentYieldsEmpty() {
		assertTrue(StringIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, character, other).isEmpty());
	}

}
