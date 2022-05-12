package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.SortedMap;

/**
 * Custom Jackson serializer for {@link SerializableValue}s that tries to reduce
 * the size of the generated json files.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ValueSerializer extends StdSerializer<SerializableValue> {

	private static final long serialVersionUID = 2323974954619016107L;

	/**
	 * Field name to represent {@link SerializableObject}s' fields when
	 * properties are present, thus preventing field-less serialization.
	 */
	static final String OBJECT_ELEMENTS_FIELD = "fields";

	/**
	 * Field name to represent {@link SerializableArray}s' elements when
	 * properties are present, thus preventing field-less serialization.
	 */
	static final String ARRAY_ELEMENTS_FIELD = "elements";

	/**
	 * Field name to represent {@link SerializableString}s' values when
	 * properties are present, thus preventing field-less serialization.
	 */
	static final String STRING_VALUE_FIELD = "value";

	/**
	 * Field name for the general properties of values.
	 */
	static final String LISA_PROPERTIES_FIELD = "lisaproperties";

	/**
	 * Builds the serializer.
	 */
	public ValueSerializer() {
		this(null);
	}

	/**
	 * Builds the serializer.
	 * 
	 * @param t the class to be serialized
	 */
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
