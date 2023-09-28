package it.unive.lisa.outputs.serializableGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;

/**
 * A single serializable value, represented in its textual form.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableString extends SerializableValue {

	private final String value;

	/**
	 * Builds an empty (invalid) string.
	 */
	public SerializableString() {
		super();
		this.value = null;
	}

	/**
	 * Builds a string.
	 * 
	 * @param properties the additional properties to use as metadata
	 * @param value      the textual representation of the value
	 */
	public SerializableString(
			SortedMap<String, String> properties,
			String value) {
		super(properties);
		this.value = value;
	}

	/**
	 * Yields the textual representation of the value.
	 * 
	 * @return the text
	 */
	public String getValue() {
		return value;
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
	public boolean equals(
			Object obj) {
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
	public int compareTo(
			SerializableValue o) {
		if (!(o instanceof SerializableString))
			// string first
			return -1;

		int cmp;
		if ((cmp = value.compareTo(((SerializableString) o).value)) != 0)
			return cmp;

		// only properties left to check
		return super.compareTo(o);
	}
}
