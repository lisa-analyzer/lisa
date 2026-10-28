package it.unive.lisa.symbolic.value.operator.ternary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeOtherType;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryTypeFixtures.FakeTypeSystem;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringStartsWithFromIndexTest {

	private final FakeTypeSystem ts = new FakeTypeSystem();
	private final Set<Type> string = Collections.singleton(ts.stringType);
	private final Set<Type> numeric = Collections.singleton(ts.integerType);
	private final Set<Type> other = Collections.singleton(new FakeOtherType());

	@Test
	public void twoStringsAndANumberYieldBoolean() {
		// regression coverage for the fixed javadoc/import bug: this operator
		// is genuinely ternary (string, string, index), not binary
		assertEquals(
				Collections.singleton(ts.booleanType),
				StringStartsWithFromIndex.INSTANCE.typeInference(ts, string, string, numeric));
	}

	@Test
	public void aNonStringFirstArgumentYieldsEmpty() {
		assertTrue(StringStartsWithFromIndex.INSTANCE.typeInference(ts, other, string, numeric).isEmpty());
	}

	@Test
	public void aNonStringSecondArgumentYieldsEmpty() {
		assertTrue(StringStartsWithFromIndex.INSTANCE.typeInference(ts, string, other, numeric).isEmpty());
	}

	@Test
	public void aNonNumericThirdArgumentYieldsEmpty() {
		assertTrue(StringStartsWithFromIndex.INSTANCE.typeInference(ts, string, string, other).isEmpty());
	}

}
