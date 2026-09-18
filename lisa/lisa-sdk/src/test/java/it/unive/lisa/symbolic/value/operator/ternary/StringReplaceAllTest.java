package it.unive.lisa.symbolic.value.operator.ternary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeOtherType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeTypeSystem;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringReplaceAllTest {

	private final FakeTypeSystem ts = new FakeTypeSystem();
	private final Set<Type> string = Collections.singleton(ts.stringType);
	private final Set<Type> other = Collections.singleton(new FakeOtherType());

	@Test
	public void allThreeStringArgumentsYieldString() {
		assertEquals(
				Collections.singleton(ts.stringType),
				StringReplaceAll.INSTANCE.typeInference(ts, string, string, string));
	}

	@Test
	public void aNonStringFirstArgumentYieldsEmpty() {
		assertTrue(StringReplaceAll.INSTANCE.typeInference(ts, other, string, string).isEmpty());
	}

	@Test
	public void aNonStringSecondArgumentYieldsEmpty() {
		assertTrue(StringReplaceAll.INSTANCE.typeInference(ts, string, other, string).isEmpty());
	}

	@Test
	public void aNonStringThirdArgumentYieldsEmpty() {
		assertTrue(StringReplaceAll.INSTANCE.typeInference(ts, string, string, other).isEmpty());
	}

}
