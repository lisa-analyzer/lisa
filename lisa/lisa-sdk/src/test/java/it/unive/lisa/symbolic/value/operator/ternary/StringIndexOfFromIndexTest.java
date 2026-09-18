package it.unive.lisa.symbolic.value.operator.ternary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeOtherType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeTypeSystem;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringIndexOfFromIndexTest {

	private final FakeTypeSystem ts = new FakeTypeSystem();
	private final Set<Type> string = Collections.singleton(ts.stringType);
	private final Set<Type> numeric = Collections.singleton(ts.integerType);
	private final Set<Type> other = Collections.singleton(new FakeOtherType());

	@Test
	public void twoStringsAndANumberYieldsInteger() {
		assertEquals(
				Collections.singleton(ts.integerType),
				StringIndexOfFromIndex.INSTANCE.typeInference(ts, string, string, numeric));
	}

	@Test
	public void aNonStringFirstArgumentYieldsEmpty() {
		assertTrue(StringIndexOfFromIndex.INSTANCE.typeInference(ts, other, string, numeric).isEmpty());
	}

	@Test
	public void aNonStringSecondArgumentYieldsEmpty() {
		assertTrue(StringIndexOfFromIndex.INSTANCE.typeInference(ts, string, other, numeric).isEmpty());
	}

	@Test
	public void aNonNumericThirdArgumentYieldsEmpty() {
		assertTrue(StringIndexOfFromIndex.INSTANCE.typeInference(ts, string, string, other).isEmpty());
	}

}
