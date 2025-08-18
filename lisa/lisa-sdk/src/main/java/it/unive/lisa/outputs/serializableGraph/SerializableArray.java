package it.unive.lisa.outputs.serializableGraph;

import it.unive.lisa.util.collections.CollectionsDiffBuilder;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import org.apache.commons.lang3.StringUtils;

/**
 * An array of serializable values, represented through a list.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SerializableArray extends SerializableValue {

	private final List<SerializableValue> elements;

	/**
	 * Builds the array.
	 */
	public SerializableArray() {
		super();
		this.elements = new LinkedList<>();
	}

	/**
	 * Builds the array.
	 * 
	 * @param properties the additional properties to use as metadata
	 * @param elements   the elements of the array
	 */
	public SerializableArray(
			SortedMap<String, String> properties,
			List<SerializableValue> elements) {
		super(properties);
		this.elements = elements;
	}

	/**
	 * Yields the elements contained in this array.
	 * 
	 * @return the elements
	 */
	public List<SerializableValue> getElements() {
		return elements;
	}

	@Override
	public Collection<SerializableValue> getInnerValues() {
		Collection<SerializableValue> result = new HashSet<>(elements);
		for (SerializableValue inner : elements)
			result.addAll(inner.getInnerValues());
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
		return "[" + StringUtils.join(elements, ", ") + "]";
	}

	@Override
	public int compareTo(
			SerializableValue o) {
		// arrays in the middle
		if (o instanceof SerializableString)
			return 1;
		if (o instanceof SerializableObject)
			return -1;

		SerializableArray other = (SerializableArray) o;
		int cmp;
		if ((cmp = Integer.compare(elements.size(), other.elements.size())) != 0)
			return cmp;

		CollectionsDiffBuilder<SerializableValue> builder = new CollectionsDiffBuilder<>(
			SerializableValue.class,
			elements,
			other.elements);
		builder.compute(SerializableValue::compareTo);

		if (builder.sameContent())
			// only properties left to check
			return super.compareTo(o);

		return builder.getOnlyFirst().iterator().next().compareTo(builder.getOnlySecond().iterator().next());
	}

}
