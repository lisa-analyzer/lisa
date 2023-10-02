package it.unive.lisa.util.representation;

import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import it.unive.lisa.util.StringUtilities;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A {@link StructuredRepresentation} in the form of a complex object with
 * fields.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ObjectRepresentation extends StructuredRepresentation {

	/**
	 * The fields of this object, with their values.
	 */
	protected final SortedMap<String, StructuredRepresentation> fields;

	/**
	 * Builds a new representation starting from the given map. {@code mapper}
	 * is used for transforming each value in the map to its individual
	 * representation.
	 * 
	 * @param <V>    the type of values in the map
	 * @param fields the map to represent
	 * @param mapper the function that knows how to convert values to their
	 *                   representation
	 */
	public <V> ObjectRepresentation(
			Map<String, V> fields,
			Function<V, StructuredRepresentation> mapper) {
		this.fields = new TreeMap<>();
		for (Entry<String, V> e : fields.entrySet())
			this.fields.put(e.getKey(), mapper.apply(e.getValue()));
	}

	/**
	 * Builds a new representation containing the given map.
	 * 
	 * @param map the map
	 */
	public ObjectRepresentation(
			Map<String, StructuredRepresentation> map) {
		if (map instanceof SortedMap)
			this.fields = (SortedMap<String, StructuredRepresentation>) map;
		else
			this.fields = new TreeMap<>(map);
	}

	@Override
	public SerializableValue toSerializableValue() {
		SortedMap<String, SerializableValue> fields = new TreeMap<>();
		for (Entry<String, StructuredRepresentation> e : this.fields.entrySet())
			fields.put(e.getKey(), e.getValue().toSerializableValue());
		return new SerializableObject(getProperties(), fields);
	}

	@Override
	public String toString() {
		if (fields.isEmpty())
			return "{}";

		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (Entry<String, StructuredRepresentation> e : fields.entrySet()) {
			String val = e.getValue().toString();
			if (!val.contains("\n"))
				sb.append(first ? "\n  " : ",\n  ")
						.append(e.getKey())
						.append(": ")
						.append(e.getValue());
			else
				sb.append(first ? "\n  " : ",\n  ")
						.append(e.getKey())
						.append(":\n")
						.append(StringUtilities.indent(val, "  ", 2));
			first = false;
		}
		sb.append("\n}");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
		ObjectRepresentation other = (ObjectRepresentation) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}
}
