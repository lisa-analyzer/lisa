package it.unive.lisa.analysis.representation;

import it.unive.lisa.outputs.serializableGraph.SerializableObject;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A {@link DomainRepresentation} in the form of a key-value mapping.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MapRepresentation extends DomainRepresentation {

	/**
	 * The mappings of contained in this map.
	 */
	protected final SortedMap<DomainRepresentation, DomainRepresentation> map;

	/**
	 * Builds a new representation starting from the given map.
	 * {@code keyMapper} and {@code valueMapper} are used for transforming each
	 * key and value in the map to their individual representation.
	 * 
	 * @param <K>         the type of keys in the map
	 * @param <V>         the type of values in the map
	 * @param map         the map to represent
	 * @param keyMapper   the function that knows how to convert keys to their
	 *                        representation
	 * @param valueMapper the function that knows how to convert values to their
	 *                        representation
	 */
	public <K, V> MapRepresentation(Map<K, V> map, Function<K, DomainRepresentation> keyMapper,
			Function<V, DomainRepresentation> valueMapper) {
		this.map = new TreeMap<>();
		for (Entry<K, V> e : map.entrySet())
			this.map.put(keyMapper.apply(e.getKey()), valueMapper.apply(e.getValue()));
	}

	/**
	 * Builds a new representation containing the given map.
	 * 
	 * @param map the map
	 */
	public MapRepresentation(Map<DomainRepresentation, DomainRepresentation> map) {
		if (map instanceof SortedMap)
			this.map = (SortedMap<DomainRepresentation, DomainRepresentation>) map;
		else
			this.map = new TreeMap<>(map);
	}

	@Override
	public SerializableValue toSerializableValue() {
		SortedMap<String, SerializableValue> fields = new TreeMap<>();
		for (Entry<DomainRepresentation, DomainRepresentation> e : this.map.entrySet())
			fields.put(e.getKey().toString(), e.getValue().toSerializableValue());
		return new SerializableObject(getProperties(), fields);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (Entry<DomainRepresentation, DomainRepresentation> e : map.entrySet())
			builder.append(e.getKey()).append(": ").append(e.getValue()).append("\n");

		return builder.toString().trim();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((map == null) ? 0 : map.hashCode());
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
		MapRepresentation other = (MapRepresentation) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}
}
