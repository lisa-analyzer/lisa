package it.unive.lisa.outputs.serializableGraph;

import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

public class SerializableObject extends SerializableValue {

	private SortedMap<String, SerializableValue> fields = new TreeMap<>();

	public SerializableObject() {
		super();
	}

	public SerializableObject(SortedMap<String, String> props, SortedMap<String, SerializableValue> fields) {
		super(props);
		this.fields = fields;
	}

	public SortedMap<String, SerializableValue> getFields() {
		return fields;
	}

	public void setFields(SortedMap<String, SerializableValue> fields) {
		this.fields = fields;
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

		if (!builder.getOnlyFirst().isEmpty())
			// same size means that both have at least one element that is
			// different
			return builder.getOnlyFirst().iterator().next().compareTo(builder.getOnlySecond().iterator().next());

		// ugly, but will do for now
		return toString().compareTo(other.toString());
	}
}
