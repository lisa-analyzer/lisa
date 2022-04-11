package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "@type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = SerializableArray.class, name = "array"),
		@JsonSubTypes.Type(value = SerializableObject.class, name = "object"),
		@JsonSubTypes.Type(value = SerializableString.class, name = "string")
})
public interface SerializableValue extends Comparable<SerializableValue> {

}
