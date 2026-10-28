package it.unive.lisa.util.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ListRepresentationTest {

	@Test
	public void testEmptyListRendersAsBrackets() {
		assertEquals("[]", new ListRepresentation().toString());
	}

	@Test
	public void testSingleLineRenderingWhenNoChildIsMultiline() {
		ListRepresentation r = new ListRepresentation(
				new StringRepresentation("a"),
				new StringRepresentation("b"));
		assertEquals("[a, b]", r.toString());
	}

	@Test
	public void testMultilineRenderingWhenAChildIsMultiline() {
		ListRepresentation nested = new ListRepresentation(
				new StringRepresentation("a"),
				new StringRepresentation("b"));
		Map<StructuredRepresentation, StructuredRepresentation> childMap = new LinkedHashMap<>();
		childMap.put(new StringRepresentation("k"), nested);
		MapRepresentation multilineChild = new MapRepresentation(childMap);
		ListRepresentation r = new ListRepresentation(new StringRepresentation("a"), multilineChild);
		String str = r.toString();
		assertTrue(str.startsWith("[\n"), "expected multiline rendering, got: " + str);
		assertTrue(str.endsWith("\n]"), "expected multiline rendering, got: " + str);
	}

	@Test
	public void testMapperConstructorAppliesMapperToEachElement() {
		ListRepresentation r = new ListRepresentation(
				List.of("a", "b", "c"),
				StringRepresentation::new);
		assertEquals("[a, b, c]", r.toString());
	}

	@Test
	public void testToSerializableValue() {
		ListRepresentation r = new ListRepresentation(new StringRepresentation("a"), new StringRepresentation("b"));
		SerializableArray value = r.toSerializableValue();
		assertEquals(2, value.getElements().size());
	}

	@Test
	public void testEqualsAndHashCode() {
		ListRepresentation r1 = new ListRepresentation(new StringRepresentation("a"));
		ListRepresentation r2 = new ListRepresentation(new StringRepresentation("a"));
		ListRepresentation r3 = new ListRepresentation(new StringRepresentation("b"));

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
		assertNotEquals(r1, r3);
	}

}
