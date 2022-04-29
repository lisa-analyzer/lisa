package it.unive.lisa.analysis.representation;

import it.unive.lisa.outputs.serializableGraph.SerializableArray;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link DomainRepresentation} in the form of a list of values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ListRepresentation extends DomainRepresentation {

	/**
	 * The elements of contained in this list.
	 */
	protected final List<DomainRepresentation> elements;

	/**
	 * Builds a new representation starting from the given list. {@code mapper}
	 * is used for transforming each element in the list to its individual
	 * representation.
	 * 
	 * @param <E>      the type of elements in the list
	 * @param elements the list to represent
	 * @param mapper   the function that knows how to convert elements to their
	 *                     representation
	 */
	public <E> ListRepresentation(List<E> elements, Function<E, DomainRepresentation> mapper) {
		this(elements.stream().map(mapper).collect(Collectors.toList()));
	}

	/**
	 * Builds a new representation containing the given list.
	 * 
	 * @param elements the list
	 */
	public ListRepresentation(List<DomainRepresentation> elements) {
		this.elements = elements;
	}

	/**
	 * Builds a new representation containing the given elements.
	 * 
	 * @param elements the list
	 */
	public ListRepresentation(DomainRepresentation... elements) {
		this.elements = Arrays.asList(elements);
	}

	@Override
	public SerializableValue toSerializableValue() {
		List<SerializableValue> values = new ArrayList<>(elements.size());
		for (DomainRepresentation e : elements)
			values.add(e.toSerializableValue());
		return new SerializableArray(getProperties(), values);
	}

	@Override
	public String toString() {
		return "[" + StringUtils.join(elements, ", ") + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
		ListRepresentation other = (ListRepresentation) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}
}
