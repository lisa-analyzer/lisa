package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.SortedMap;

public class ValueSerializer extends StdSerializer<SerializableValue> {

	private static final long serialVersionUID = 2323974954619016107L;

	static final String OBJECT_ELEMENTS_FIELD = "fields";
	static final String ARRAY_ELEMENTS_FIELD = "elements";
	static final String STRING_VALUE_FIELD = "value";
	static final String LISA_PROPERTIES_FIELD = "lisaproperties";

	public ValueSerializer() {
		this(null);
	}

	public ValueSerializer(Class<SerializableValue> t) {
		super(t);
	}

	@Override
	public void serialize(SerializableValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		SortedMap<String, String> props = value.getProperties();
		boolean hasProps = !props.isEmpty();
		if (hasProps) {
			gen.writeStartObject();
			provider.defaultSerializeField(LISA_PROPERTIES_FIELD, props, gen);
		}

		if (value instanceof SerializableString)
			if (hasProps)
				provider.defaultSerializeField(STRING_VALUE_FIELD, ((SerializableString) value).getValue(), gen);
			else
				provider.defaultSerializeValue(((SerializableString) value).getValue(), gen);
		else if (value instanceof SerializableArray)
			if (hasProps)
				provider.defaultSerializeField(ARRAY_ELEMENTS_FIELD, ((SerializableArray) value).getElements(), gen);
			else
				provider.defaultSerializeValue(((SerializableArray) value).getElements(), gen);
		else if (value instanceof SerializableObject)
			if (hasProps)
				provider.defaultSerializeField(OBJECT_ELEMENTS_FIELD, ((SerializableObject) value).getFields(), gen);
			else
				provider.defaultSerializeValue(((SerializableObject) value).getFields(), gen);
		else
			throw new IllegalArgumentException("Unknown value type: " + value.getClass().getName());

		if (hasProps)
			gen.writeEndObject();
	}
}
