package it.unive.lisa.outputs.serializableGraph;

import java.util.LinkedList;
import java.util.List;

import it.unive.lisa.util.collections.CollectionsDiffBuilder;

public class SerializableArray implements SerializableValue {

	private List<SerializableValue> elements = new LinkedList<>();

	public SerializableArray() {
	}

	public SerializableArray(List<SerializableValue> elements) {
		this.elements = elements;
	}

	public List<SerializableValue> getElements() {
		return elements;
	}

	public void setElements(List<SerializableValue> elements) {
		this.elements = elements;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
		SerializableArray other = (SerializableArray) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JsonArray [elements=" + elements + "]";
	}

	@Override
	public int compareTo(SerializableValue o) {
		// arrays in the middle
		if (o instanceof SerializableString)
			return 1;
		if (o instanceof SerializableObject)
			return -1;

		SerializableArray other = (SerializableArray) o;
		int cmp;
		if ((cmp = Integer.compare(elements.size(), other.elements.size())) != 0)
			return cmp;

		CollectionsDiffBuilder<
				SerializableValue> builder = new CollectionsDiffBuilder<>(SerializableValue.class, elements, other.elements);
		builder.compute(SerializableValue::compareTo);

		if (builder.sameContent())
			return 0;

		return builder.getOnlyFirst().iterator().next().compareTo(builder.getOnlySecond().iterator().next());
	}
}
