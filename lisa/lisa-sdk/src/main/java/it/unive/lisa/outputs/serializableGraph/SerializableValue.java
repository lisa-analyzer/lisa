package it.unive.lisa.outputs.serializableGraph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A value that can be serialized, as part of a
 * {@link SerializableNodeDescription}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@JsonSerialize(using = ValueSerializer.class)
@JsonDeserialize(using = ValueDeserializer.class)
public abstract class SerializableValue implements Comparable<SerializableValue> {

	private final SortedMap<String, String> properties;

	/**
	 * Builds the value.
	 */
	protected SerializableValue() {
		this.properties = new TreeMap<>();
	}

	/**
	 * Builds the value.
	 * 
	 * @param properties the additional properties to use as metadata
	 */
	protected SerializableValue(SortedMap<String, String> properties) {
		this.properties = properties;
	}

	/**
	 * Yields the additional properties to use as metadata.
	 * 
	 * @return the properties
	 */
	public SortedMap<String, String> getProperties() {
		return properties;
	}

	/**
	 * Sets a textual property to enrich the information represented by this
	 * value.
	 * 
	 * @param key   the key of the property
	 * @param value the value of the property
	 */
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}

	/**
	 * Yields all the {@link SerializableValue}s that are contained into this
	 * one, recursively. The receiver of this call is always excluded by the
	 * returned collection.
	 * 
	 * @return the values contained into this one, recursively
	 */
	@JsonIgnore
	public abstract Collection<SerializableValue> getInnerValues();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

	@Override
	public int compareTo(SerializableValue o) {
		int cmp;
		if ((cmp = Integer.compare(properties.keySet().size(), o.properties.keySet().size())) != 0)
			return cmp;

		CollectionsDiffBuilder<
				String> builder = new CollectionsDiffBuilder<>(String.class, properties.keySet(),
						o.properties.keySet());
		builder.compute(String::compareTo);

		if (!builder.sameContent())
			// same size means that both have at least one element that is
			// different
			return builder.getOnlyFirst().iterator().next().compareTo(builder.getOnlySecond().iterator().next());

		// same keys: just iterate over them and apply comparisons
		// since fields is sorted, the order of iteration will be consistent
		for (Entry<String, String> entry : properties.entrySet())
			if ((cmp = entry.getValue().compareTo(o.properties.get(entry.getKey()))) != 0)
				return cmp;

		return 0;
	}
}
