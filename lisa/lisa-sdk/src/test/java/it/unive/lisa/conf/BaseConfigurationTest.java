package it.unive.lisa.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BaseConfigurationTest {

	@SuppressWarnings("unused")
	private static class FakeConfiguration
			extends
			BaseConfiguration {

		public int number = 0;
		public String text = "a";
		public static final int IGNORED_STATIC = 42;
		private int ignoredPrivate = 1;

		public FakeConfiguration() {
		}
	}

	@SuppressWarnings("unused")
	private static class OtherConfiguration
			extends
			BaseConfiguration {

		public int number = 0;

		public OtherConfiguration() {
		}
	}

	@Test
	public void sameValuesInAllPublicInstanceFieldsAreEqual() {
		FakeConfiguration a = new FakeConfiguration();
		FakeConfiguration b = new FakeConfiguration();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void differentValueInAnyPublicInstanceFieldMakesThemUnequal() {
		FakeConfiguration a = new FakeConfiguration();
		FakeConfiguration b = new FakeConfiguration();
		b.number = 1;
		assertFalse(a.equals(b));

		FakeConfiguration c = new FakeConfiguration();
		c.text = "b";
		assertFalse(a.equals(c));
	}

	@Test
	public void staticAndPrivateFieldsAreIgnored() {
		FakeConfiguration a = new FakeConfiguration();
		FakeConfiguration b = new FakeConfiguration();
		b.ignoredPrivate = 999;
		// only the private field differs, which reflection over getFields()
		// cannot even see (it only returns public members), so the two must
		// still compare equal
		assertTrue(a.equals(b));
	}

	@Test
	public void differentConcreteClassesAreNeverEqualEvenWithTheSameFieldValues() {
		FakeConfiguration a = new FakeConfiguration();
		OtherConfiguration b = new OtherConfiguration();
		assertFalse(a.equals(b));
	}

	@Test
	public void equalsHandlesNullAndSelfPerTheStandardContract() {
		FakeConfiguration a = new FakeConfiguration();
		assertTrue(a.equals(a));
		assertFalse(a.equals(null));
	}

}
