package it.unive.lisa.util.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class ObjectRepresentationTest {

	@Test
	public void testEmptyObjectRendersAsBraces() {
		assertEquals("{}", new ObjectRepresentation(new TreeMap<>()).toString());
	}

	@Test
	public void testFieldsAreRenderedInKeySortedOrder() {
		Map<String, StructuredRepresentation> fields = new TreeMap<>();
		fields.put("z", new StringRepresentation("1"));
		fields.put("a", new StringRepresentation("2"));
		ObjectRepresentation r = new ObjectRepresentation(fields);
		assertEquals("{\n  a: 2,\n  z: 1\n}", r.toString());
	}

	@Test
	public void testMultilineRenderingForCompositeField() {
		ListRepresentation compositeValue = new ListRepresentation(
				new StringRepresentation("a"),
				new StringRepresentation("b"));
		Map<String, StructuredRepresentation> inner = new TreeMap<>();
		inner.put("nested", compositeValue);
		Map<String, StructuredRepresentation> fields = new TreeMap<>();
		fields.put("field", new ObjectRepresentation(inner));

		ObjectRepresentation r = new ObjectRepresentation(fields);
		String str = r.toString();
		assertTrue(str.contains("field:\n"),
				"expected the multiline value to be rendered on its own line, got: " + str);
	}

	@Test
	public void testMapperConstructorAppliesMapper() {
		Map<String, Integer> src = new LinkedHashMap<>();
		src.put("a", 1);
		src.put("b", 2);
		ObjectRepresentation r = new ObjectRepresentation(src, StringRepresentation::new);
		assertEquals("{\n  a: 1,\n  b: 2\n}", r.toString());
	}

	@Test
	public void testToSerializableValue() {
		Map<String, StructuredRepresentation> fields = new TreeMap<>();
		fields.put("k", new StringRepresentation("v"));
		SerializableObject value = new ObjectRepresentation(fields).toSerializableValue();
		assertEquals(1, value.getFields().size());
	}

	@Test
	public void testEqualsAndHashCode() {
		Map<String, StructuredRepresentation> f1 = new TreeMap<>();
		f1.put("k", new StringRepresentation("v"));
		Map<String, StructuredRepresentation> f2 = new TreeMap<>();
		f2.put("k", new StringRepresentation("v"));
		Map<String, StructuredRepresentation> f3 = new TreeMap<>();
		f3.put("k", new StringRepresentation("other"));

		ObjectRepresentation r1 = new ObjectRepresentation(f1);
		ObjectRepresentation r2 = new ObjectRepresentation(f2);
		ObjectRepresentation r3 = new ObjectRepresentation(f3);

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
		assertNotEquals(r1, r3);
	}

}
