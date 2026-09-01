package it.unive.lisa.symbolic.value.operator.ternary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeOtherType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeTypeSystem;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringLastIndexOfCharFromIndexTest {

	private final FakeTypeSystem ts = new FakeTypeSystem();
	private final Set<Type> string = Collections.singleton(ts.stringType);
	private final Set<Type> character = Collections.singleton(ts.characterType);
	private final Set<Type> numeric = Collections.singleton(ts.integerType);
	private final Set<Type> other = Collections.singleton(new FakeOtherType());

	@Test
	public void stringCharAndNumberYieldInteger() {
		assertEquals(
				Collections.singleton(ts.integerType),
				StringLastIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, character, numeric));
	}

	@Test
	public void aNonStringFirstArgumentYieldsEmpty() {
		assertTrue(StringLastIndexOfCharFromIndex.INSTANCE.typeInference(ts, other, character, numeric).isEmpty());
	}

	@Test
	public void aNonCharacterSecondArgumentYieldsEmpty() {
		assertTrue(StringLastIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, string, numeric).isEmpty());
		assertTrue(StringLastIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, other, numeric).isEmpty());
	}

	@Test
	public void aNonNumericThirdArgumentYieldsEmpty() {
		assertTrue(StringLastIndexOfCharFromIndex.INSTANCE.typeInference(ts, string, character, other).isEmpty());
	}

}
