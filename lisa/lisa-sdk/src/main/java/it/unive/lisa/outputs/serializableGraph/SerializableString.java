package it.unive.lisa.outputs.serializableGraph;

import it.unive.lisa.util.collections.CollectionUtilities;

public class SerializableString implements SerializableValue {

	private String value = null;

	public SerializableString() {
	}

	public SerializableString(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		return "JsonString [value=" + value + "]";
	}

	@Override
	public int compareTo(SerializableValue o) {
		if (!(o instanceof SerializableString))
			// string first
			return -1;
		return CollectionUtilities.nullSafeCompare(true, value, ((SerializableString) o).value, String::compareTo);
	}
}
