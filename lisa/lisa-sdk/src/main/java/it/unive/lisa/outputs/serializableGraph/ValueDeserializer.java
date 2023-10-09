package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Custom Jackson deserializer for {@link SerializableValue}s, built to read
 * back what is serialized by {@link ValueSerializer}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ValueDeserializer extends StdDeserializer<SerializableValue> {

	private static final long serialVersionUID = -5817422267160292909L;

	/**
	 * Builds the deserializer.
	 */
	public ValueDeserializer() {
		this(null);
	}

	/**
	 * Builds the deserializer.
	 * 
	 * @param t the class to be deserialized
	 */
	public ValueDeserializer(
			Class<SerializableValue> t) {
		super(t);
	}

	@Override
	public SerializableValue deserialize(
			JsonParser p,
			DeserializationContext ctxt)
			throws IOException,
			JsonProcessingException {
		JsonNode node = p.getCodec().readTree(p);
		JsonNode props = node.get(ValueSerializer.LISA_PROPERTIES_FIELD);
		SortedMap<String, String> properties = new TreeMap<>();
		if (props != null) {
			Iterator<Entry<String, JsonNode>> fields = props.fields();
			while (fields.hasNext()) {
				Entry<String, JsonNode> field = fields.next();
				properties.put(field.getKey(), field.getValue().asText());
			}

			JsonNode arr = node.get(ValueSerializer.ARRAY_ELEMENTS_FIELD);
			JsonNode obj = node.get(ValueSerializer.OBJECT_ELEMENTS_FIELD);
			JsonNode str = node.get(ValueSerializer.STRING_VALUE_FIELD);

			if (str != null && arr == null && obj == null)
				return new SerializableString(properties, str.asText());
			else if (str == null && arr != null && obj == null) {
				Iterator<JsonNode> elements = arr.elements();
				List<SerializableValue> list = new LinkedList<>();
				while (elements.hasNext()) {
					JsonNode element = elements.next();
					list.add(ctxt.readValue(element.traverse(p.getCodec()), SerializableValue.class));
				}
				return new SerializableArray(properties, list);
			} else if (str == null && arr == null && obj != null) {
				Iterator<Entry<String, JsonNode>> objFields = obj.fields();
				SortedMap<String, SerializableValue> map = new TreeMap<>();
				while (objFields.hasNext()) {
					Entry<String, JsonNode> objField = objFields.next();
					map.put(objField.getKey(),
							ctxt.readValue(objField.getValue().traverse(p.getCodec()), SerializableValue.class));
				}
				return new SerializableObject(properties, map);
			} else
				throw new IllegalArgumentException("Node type not supported");
		} else
			switch (node.getNodeType()) {
			case ARRAY:
				Iterator<JsonNode> elements = node.elements();
				List<SerializableValue> list = new LinkedList<>();
				while (elements.hasNext()) {
					JsonNode element = elements.next();
					list.add(ctxt.readValue(element.traverse(p.getCodec()), SerializableValue.class));
				}
				return new SerializableArray(properties, list);
			case OBJECT:
				Iterator<Entry<String, JsonNode>> objFields = node.fields();
				SortedMap<String, SerializableValue> map = new TreeMap<>();
				while (objFields.hasNext()) {
					Entry<String, JsonNode> objField = objFields.next();
					map.put(objField.getKey(),
							ctxt.readValue(objField.getValue().traverse(p.getCodec()), SerializableValue.class));
				}
				return new SerializableObject(properties, map);
			case STRING:
				return new SerializableString(properties, node.asText());
			case BINARY:
			case BOOLEAN:
			case MISSING:
			case NULL:
			case NUMBER:
			case POJO:
			default:
				throw new IllegalArgumentException("Node type not supported: " + node.getNodeType());
			}
	}
}
