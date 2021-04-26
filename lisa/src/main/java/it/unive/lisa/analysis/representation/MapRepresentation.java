package it.unive.lisa.analysis.representation;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

public class MapRepresentation extends DomainRepresentation {

	private final SortedMap<DomainRepresentation, DomainRepresentation> map;

	public <K, V> MapRepresentation(Map<K, V> map, Function<K, DomainRepresentation> keyMapper,
			Function<V, DomainRepresentation> valueMapper) {
		this.map = new TreeMap<>();
		for (Entry<K, V> e : map.entrySet())
			this.map.put(keyMapper.apply(e.getKey()), valueMapper.apply(e.getValue()));
	}

	public MapRepresentation(Map<DomainRepresentation, DomainRepresentation> map) {
		if (map instanceof SortedMap)
			this.map = (SortedMap<DomainRepresentation, DomainRepresentation>) map;
		else
			this.map = new TreeMap<>(map);
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
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
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
		MapRepresentation other = (MapRepresentation) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}
}
