package it.unive.lisa.util.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class MapRepresentationTest {

	@Test
	public void testEmptyMapRendersAsBraces() {
		Map<StructuredRepresentation, StructuredRepresentation> empty = new TreeMap<>();
		assertEquals("{}", new MapRepresentation(empty).toString());
	}

	@Test
	public void testSingleLineRenderingForSimpleEntries() {
		Map<StructuredRepresentation, StructuredRepresentation> map = new TreeMap<>();
		map.put(new StringRepresentation("k1"), new StringRepresentation("v1"));
		map.put(new StringRepresentation("k2"), new StringRepresentation("v2"));
		MapRepresentation r = new MapRepresentation(map);
		assertEquals("{\n  k1: v1,\n  k2: v2\n}", r.toString());
	}

	@Test
	public void testMultilineRenderingForCompositeValue() {
		ListRepresentation compositeValue = new ListRepresentation(
				new StringRepresentation("a"),
				new StringRepresentation("b"));
		Map<StructuredRepresentation, StructuredRepresentation> inner = new TreeMap<>();
		inner.put(new StringRepresentation("nested"), compositeValue);
		Map<StructuredRepresentation, StructuredRepresentation> map = new TreeMap<>();
		map.put(new StringRepresentation("k"), new MapRepresentation(inner));

		MapRepresentation r = new MapRepresentation(map);
		String str = r.toString();
		assertTrue(str.contains("k:\n"), "expected the multiline value to be rendered on its own line, got: " + str);
	}

	@Test
	public void testMapperConstructorAppliesMappers() {
		Map<String, Integer> src = new LinkedHashMap<>();
		src.put("a", 1);
		src.put("b", 2);
		MapRepresentation r = new MapRepresentation(src, StringRepresentation::new, StringRepresentation::new);
		assertEquals("{\n  a: 1,\n  b: 2\n}", r.toString());
	}

	@Test
	public void testToSerializableValue() {
		Map<StructuredRepresentation, StructuredRepresentation> map = new TreeMap<>();
		map.put(new StringRepresentation("k"), new StringRepresentation("v"));
		SerializableObject value = new MapRepresentation(map).toSerializableValue();
		assertEquals(1, value.getFields().size());
		assertEquals("v", ((it.unive.lisa.outputs.serializableGraph.SerializableString) value.getFields().get("k"))
				.getValue());
	}

	@Test
	public void testEqualsAndHashCode() {
		Map<StructuredRepresentation, StructuredRepresentation> m1 = new TreeMap<>();
		m1.put(new StringRepresentation("k"), new StringRepresentation("v"));
		Map<StructuredRepresentation, StructuredRepresentation> m2 = new TreeMap<>();
		m2.put(new StringRepresentation("k"), new StringRepresentation("v"));
		Map<StructuredRepresentation, StructuredRepresentation> m3 = new TreeMap<>();
		m3.put(new StringRepresentation("k"), new StringRepresentation("other"));

		MapRepresentation r1 = new MapRepresentation(m1);
		MapRepresentation r2 = new MapRepresentation(m2);
		MapRepresentation r3 = new MapRepresentation(m3);

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
		assertNotEquals(r1, r3);
	}

}
