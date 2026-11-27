package it.unive.lisa.util.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.outputs.serializableGraph.SerializableString;
import org.junit.jupiter.api.Test;

public class StringRepresentationTest {

	@Test
	public void testStringConstructorKeepsValueVerbatim() {
		StringRepresentation r = new StringRepresentation("foo");
		assertEquals("foo", r.toString());
	}

	@Test
	public void testObjectConstructorUsesStringValueOf() {
		StringRepresentation r = new StringRepresentation(42);
		assertEquals("42", r.toString());

		StringRepresentation nullRepr = new StringRepresentation((Object) null);
		assertEquals("null", nullRepr.toString());
	}

	@Test
	public void testToSerializableValue() {
		StringRepresentation r = new StringRepresentation("foo");
		SerializableString value = r.toSerializableValue();
		assertEquals("foo", value.getValue());
		assertEquals(r.getProperties(), value.getProperties());
	}

	@Test
	public void testEqualsAndHashCode() {
		StringRepresentation r1 = new StringRepresentation("foo");
		StringRepresentation r2 = new StringRepresentation("foo");
		StringRepresentation r3 = new StringRepresentation("bar");

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
		assertNotEquals(r1, r3);
		assertNotEquals(r1, new ListRepresentation());
	}

	@Test
	public void testCompareToUsesToString() {
		StringRepresentation a = new StringRepresentation("a");
		StringRepresentation b = new StringRepresentation("b");
		assertEquals(true, a.compareTo(b) < 0);
		assertEquals(true, b.compareTo(a) > 0);
		assertEquals(0, a.compareTo(new StringRepresentation("a")));
		assertEquals(1, a.compareTo(null));
	}

}
