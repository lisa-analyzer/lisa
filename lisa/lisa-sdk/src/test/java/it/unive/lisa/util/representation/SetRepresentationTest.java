package it.unive.lisa.util.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class SetRepresentationTest {

	@Test
	public void testEmptySetRendersAsParens() {
		assertEquals("()", new SetRepresentation(Set.of()).toString());
	}

	@Test
	public void testElementsAreRenderedInSortedOrder() {
		Set<StructuredRepresentation> elements = Set.of(
				new StringRepresentation("b"),
				new StringRepresentation("a"),
				new StringRepresentation("c"));
		SetRepresentation r = new SetRepresentation(elements);
		assertEquals("(a, b, c)", r.toString());
	}

	@Test
	public void testAlreadySortedSetIsReusedNotCopied() {
		TreeSet<StructuredRepresentation> sorted = new TreeSet<>();
		sorted.add(new StringRepresentation("a"));
		SetRepresentation r = new SetRepresentation(sorted);
		assertEquals("(a)", r.toString());
	}

	@Test
	public void testMapperConstructorAppliesMapperAndSorts() {
		SetRepresentation r = new SetRepresentation(Set.of("b", "a"), StringRepresentation::new);
		assertEquals("(a, b)", r.toString());
	}

	@Test
	public void testToSerializableValue() {
		SetRepresentation r = new SetRepresentation(Set.of(new StringRepresentation("a")));
		SerializableArray value = r.toSerializableValue();
		assertEquals(1, value.getElements().size());
	}

	@Test
	public void testEqualsAndHashCode() {
		SetRepresentation r1 = new SetRepresentation(Set.of(new StringRepresentation("a")));
		SetRepresentation r2 = new SetRepresentation(Set.of(new StringRepresentation("a")));
		SetRepresentation r3 = new SetRepresentation(Set.of(new StringRepresentation("b")));

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
		assertNotEquals(r1, r3);
	}

}
