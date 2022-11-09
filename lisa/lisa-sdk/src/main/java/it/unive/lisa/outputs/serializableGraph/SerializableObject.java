package it.unive.lisa.outputs.serializableGraph;

import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A complex serializable object, represented through a set of named
 * serializable fields.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableObject extends SerializableValue {

	private final SortedMap<String, SerializableValue> fields;

	/**
	 * Builds an empty object.
	 */
	public SerializableObject() {
		super();
		this.fields = new TreeMap<>();
	}

	/**
	 * Builds an object.
	 * 
	 * @param properties the additional properties to use as metadata
	 * @param fields     the fields of this object
	 */
	public SerializableObject(SortedMap<String, String> properties, SortedMap<String, SerializableValue> fields) {
		super(properties);
		this.fields = fields;
	}

	/**
	 * Yields the fields of this object.
	 * 
	 * @return the fields
	 */
	public SortedMap<String, SerializableValue> getFields() {
		return fields;
	}

	@Override
	public Collection<SerializableValue> getInnerValues() {
		Collection<SerializableValue> result = new HashSet<>(fields.values());
		for (SerializableValue inner : fields.values())
			result.addAll(inner.getInnerValues());
		return result;
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
		SerializableObject other = (SerializableObject) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, SerializableValue> entry : fields.entrySet())
			builder.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
		if (builder.length() > 0)
			builder.delete(builder.length() - 2, builder.length());
		return "{ " + builder.toString() + " }";
	}

	@Override
	public int compareTo(SerializableValue o) {
		if (!(o instanceof SerializableObject))
			// maps last
			return 1;

		SerializableObject other = (SerializableObject) o;
		int cmp;
		if ((cmp = Integer.compare(fields.keySet().size(), other.fields.keySet().size())) != 0)
			return cmp;

		CollectionsDiffBuilder<
				String> builder = new CollectionsDiffBuilder<>(String.class, fields.keySet(), other.fields.keySet());
		builder.compute(String::compareTo);

		if (!builder.sameContent())
			// same size means that both have at least one element that is
			// different
			return builder.getOnlyFirst().iterator().next().compareTo(builder.getOnlySecond().iterator().next());

		// same keys: just iterate over them and apply comparisons
		// since fields is sorted, the order of iteration will be consistent
		for (Entry<String, SerializableValue> entry : this.fields.entrySet())
			if ((cmp = entry.getValue().compareTo(other.fields.get(entry.getKey()))) != 0)
				return cmp;

		// only properties left to check
		return super.compareTo(o);
	}
}
