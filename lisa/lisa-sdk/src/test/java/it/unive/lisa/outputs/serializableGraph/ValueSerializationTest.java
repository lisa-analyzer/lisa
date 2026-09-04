package it.unive.lisa.outputs.serializableGraph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

// round-trip tests for ValueSerializer/ValueDeserializer, exercised through
// the full Jackson pipeline (not by calling them directly), since that is
// how they are actually wired via @JsonSerialize/@JsonDeserialize on
// SerializableValue
public class ValueSerializationTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static <T extends SerializableValue> T roundTrip(
			T value)
			throws IOException {
		String json = MAPPER.writeValueAsString(value);
		@SuppressWarnings("unchecked")
		T result = (T) MAPPER.readValue(json, SerializableValue.class);
		return result;
	}

	@Test
	public void plainStringRoundTrips()
			throws IOException {
		SerializableString original = new SerializableString(new TreeMap<>(), "hello");
		SerializableValue result = roundTrip(original);
		assertEquals(original, result);
	}

	@Test
	public void stringWithPropertiesRoundTrips()
			throws IOException {
		TreeMap<String, String> props = new TreeMap<>();
		props.put("color", "red");
		SerializableString original = new SerializableString(props, "hello");
		SerializableValue result = roundTrip(original);
		assertEquals(original, result);
	}

	@Test
	public void plainArrayRoundTrips()
			throws IOException {
		SerializableArray original = new SerializableArray(
				new TreeMap<>(),
				List.of(new SerializableString(new TreeMap<>(), "a"), new SerializableString(new TreeMap<>(), "b")));
		SerializableValue result = roundTrip(original);
		assertEquals(original, result);
	}

	@Test
	public void arrayWithPropertiesRoundTrips()
			throws IOException {
		TreeMap<String, String> props = new TreeMap<>();
		props.put("length", "2");
		SerializableArray original = new SerializableArray(
				props,
				List.of(new SerializableString(new TreeMap<>(), "a")));
		SerializableValue result = roundTrip(original);
		assertEquals(original, result);
	}

	@Test
	public void plainObjectRoundTrips()
			throws IOException {
		TreeMap<String, SerializableValue> fields = new TreeMap<>();
		fields.put("name", new SerializableString(new TreeMap<>(), "test"));
		fields.put("value", new SerializableString(new TreeMap<>(), "42"));
		SerializableObject original = new SerializableObject(new TreeMap<>(), fields);
		SerializableValue result = roundTrip(original);
		assertEquals(original, result);
	}

	@Test
	public void objectWithPropertiesRoundTrips()
			throws IOException {
		TreeMap<String, String> props = new TreeMap<>();
		props.put("kind", "record");
		TreeMap<String, SerializableValue> fields = new TreeMap<>();
		fields.put("name", new SerializableString(new TreeMap<>(), "test"));
		SerializableObject original = new SerializableObject(props, fields);
		SerializableValue result = roundTrip(original);
		assertEquals(original, result);
	}

	@Test
	public void nestedStructuresRoundTrip()
			throws IOException {
		TreeMap<String, SerializableValue> innerFields = new TreeMap<>();
		innerFields.put("inner", new SerializableString(new TreeMap<>(), "value"));
		SerializableObject innerObject = new SerializableObject(new TreeMap<>(), innerFields);

		SerializableArray array = new SerializableArray(
				new TreeMap<>(),
				List.of(innerObject, new SerializableString(new TreeMap<>(), "sibling")));

		TreeMap<String, SerializableValue> outerFields = new TreeMap<>();
		outerFields.put("items", array);
		SerializableObject original = new SerializableObject(new TreeMap<>(), outerFields);

		SerializableValue result = roundTrip(original);
		assertEquals(original, result);
	}

	@Test
	public void emptyArrayAndObjectRoundTrip()
			throws IOException {
		SerializableArray emptyArray = new SerializableArray();
		assertEquals(emptyArray, roundTrip(emptyArray));

		SerializableObject emptyObject = new SerializableObject();
		assertEquals(emptyObject, roundTrip(emptyObject));
	}

	@Test
	public void roundTripThroughASerializableGraphPreservesNestedDescriptions()
			throws IOException {
		TreeMap<String, SerializableValue> fields = new TreeMap<>();
		fields.put("a", new SerializableArray(new TreeMap<>(), List.of(new SerializableString(new TreeMap<>(), "x"))));
		SerializableObject description = new SerializableObject(new TreeMap<>(), fields);

		SerializableGraph original = new SerializableGraph(
				"g", null, new TreeSet<>(), new TreeSet<>(),
				new TreeSet<>(List.of(new SerializableNodeDescription(1, description))));

		java.io.StringWriter writer = new java.io.StringWriter();
		original.dump(writer);
		SerializableGraph roundTripped = SerializableGraph.readGraph(new java.io.StringReader(writer.toString()));

		assertEquals(original, roundTripped);
	}

}
