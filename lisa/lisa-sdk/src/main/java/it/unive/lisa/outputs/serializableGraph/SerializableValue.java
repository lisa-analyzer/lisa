package it.unive.lisa.outputs.serializableGraph;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "@type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = SerializableArray.class, name = "array"),
		@JsonSubTypes.Type(value = SerializableObject.class, name = "object"),
		@JsonSubTypes.Type(value = SerializableString.class, name = "string")
})
public abstract class SerializableValue implements Comparable<SerializableValue> {

	private SortedMap<String, String> props = new TreeMap<>();
	
	protected SerializableValue() {}
	
	protected SerializableValue(SortedMap<String, String> props) {
		this.props = props;
	}

	@JsonInclude(value = Include.NON_EMPTY)
	public SortedMap<String, String> getProps() {
		return props;
	}

	public void setProps(SortedMap<String, String> props) {
		this.props = props;
	}

	public void setProperty(String key, String value) {
		if (props == null)
			props = new TreeMap<>();
		props.put(key, value);
	}
	
	@JsonIgnore
	public abstract Collection<SerializableValue> getInnerValues();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((props == null) ? 0 : props.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SerializableValue other = (SerializableValue) obj;
		if (props == null) {
			if (other.props != null)
				return false;
		} else if (!props.equals(other.props))
			return false;
		return true;
	}
}
