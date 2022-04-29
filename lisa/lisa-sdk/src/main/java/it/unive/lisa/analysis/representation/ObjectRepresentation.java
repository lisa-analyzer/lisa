package it.unive.lisa.analysis.representation;

import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A {@link DomainRepresentation} in the form of a complex object with fields.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ObjectRepresentation extends DomainRepresentation {

	/**
	 * The fields of this object, with their values.
	 */
	protected final SortedMap<String, DomainRepresentation> fields;

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
	public <V> ObjectRepresentation(Map<String, V> fields, Function<V, DomainRepresentation> mapper) {
		this.fields = new TreeMap<>();
		for (Entry<String, V> e : fields.entrySet())
			this.fields.put(e.getKey(), mapper.apply(e.getValue()));
	}

	/**
	 * Builds a new representation containing the given map.
	 * 
	 * @param map the map
	 */
	public ObjectRepresentation(Map<String, DomainRepresentation> map) {
		if (map instanceof SortedMap)
			this.fields = (SortedMap<String, DomainRepresentation>) map;
		else
			this.fields = new TreeMap<>(map);
	}

	@Override
	public SerializableValue toSerializableValue() {
		SortedMap<String, SerializableValue> fields = new TreeMap<>();
		for (Entry<String, DomainRepresentation> e : this.fields.entrySet())
			fields.put(e.getKey(), e.getValue().toSerializableValue());
		return new SerializableObject(getProperties(), fields);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (Entry<String, DomainRepresentation> e : fields.entrySet())
			builder.append(e.getKey()).append(": ").append(e.getValue()).append("\n");

		return builder.toString().trim();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
		ObjectRepresentation other = (ObjectRepresentation) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}
}
