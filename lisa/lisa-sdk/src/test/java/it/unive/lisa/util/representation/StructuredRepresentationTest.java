package it.unive.lisa.util.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StructuredRepresentationTest {

	@Test
	public void testPropertiesStartEmptyAndAreMutable() {
		StringRepresentation r = new StringRepresentation("foo");
		assertTrue(r.getProperties().isEmpty());
		r.setProperty("k", "v");
		assertEquals("v", r.getProperties().get("k"));
	}

	@Test
	public void testPropertiesContributeToEqualsAndHashCode() {
		StringRepresentation r1 = new StringRepresentation("foo");
		StringRepresentation r2 = new StringRepresentation("foo");
		assertEquals(r1, r2);

		r1.setProperty("k", "v");
		assertNotEquals(r1, r2);

		r2.setProperty("k", "v");
		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
	}

	@Test
	public void testCompareToOrdersByClassNameWhenClassesDiffer() {
		StringRepresentation s = new StringRepresentation("z");
		ListRepresentation l = new ListRepresentation();
		int expected = StringRepresentation.class.getName().compareTo(ListRepresentation.class.getName());
		assertEquals(Integer.signum(expected), Integer.signum(s.compareTo(l)));
		assertEquals(Integer.signum(-expected), Integer.signum(l.compareTo(s)));
	}

	@Test
	public void testCompareToWithNullYieldsPositive() {
		assertTrue(new StringRepresentation("a").compareTo(null) > 0);
	}

}
