package it.unive.lisa.symbolic.value.operator.ternary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeNumericType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeOtherType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeTypeSystem;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringSubstringTest {

	private final FakeTypeSystem ts = new FakeTypeSystem();
	private final Set<Type> string = Collections.singleton(ts.stringType);
	private final Set<Type> integral = Collections.singleton(ts.integerType);
	private final Set<Type> nonIntegral = Collections.singleton(new FakeNumericType(false));
	private final Set<Type> other = Collections.singleton(new FakeOtherType());

	@Test
	public void stringWithTwoIntegralBoundsYieldsString() {
		assertEquals(
				Collections.singleton(ts.stringType),
				StringSubstring.INSTANCE.typeInference(ts, string, integral, integral));
	}

	@Test
	public void aNonStringFirstArgumentYieldsEmpty() {
		assertTrue(StringSubstring.INSTANCE.typeInference(ts, other, integral, integral).isEmpty());
	}

	@Test
	public void aNonIntegralSecondArgumentYieldsEmpty() {
		// a non-integral (e.g. floating point) numeric type does not qualify
		// as a valid string index, even though it is numeric
		assertTrue(StringSubstring.INSTANCE.typeInference(ts, string, nonIntegral, integral).isEmpty());
		assertTrue(StringSubstring.INSTANCE.typeInference(ts, string, other, integral).isEmpty());
	}

	@Test
	public void aNonIntegralThirdArgumentYieldsEmpty() {
		assertTrue(StringSubstring.INSTANCE.typeInference(ts, string, integral, nonIntegral).isEmpty());
		assertTrue(StringSubstring.INSTANCE.typeInference(ts, string, integral, other).isEmpty());
	}

}
