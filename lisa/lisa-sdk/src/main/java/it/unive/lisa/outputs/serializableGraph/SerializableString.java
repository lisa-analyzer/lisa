package it.unive.lisa.outputs.serializableGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;

import it.unive.lisa.util.collections.CollectionUtilities;

public class SerializableString extends SerializableValue {

	private String value = null;

	public SerializableString() {
		super();
	}

	public SerializableString(SortedMap<String, String> props, String value) {
		super(props);
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public Collection<SerializableValue> getInnerValues() {
		return Collections.emptySet();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SerializableString other = (SerializableString) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int compareTo(SerializableValue o) {
		if (!(o instanceof SerializableString))
			// string first
			return -1;
		return CollectionUtilities.nullSafeCompare(true, value, ((SerializableString) o).value, String::compareTo);
	}
}
