package it.unive.lisa.analysis.representation;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.util.collections.IterableArray;

public class SetRepresentation extends DomainRepresentation {

	private final SortedSet<DomainRepresentation> elements;

	@SafeVarargs
	public <E> SetRepresentation(Function<E, DomainRepresentation> mapper, E... elements) {
		this(mapAndSort(new IterableArray<>(elements), mapper));
	}

	public <E> SetRepresentation(Collection<E> elements, Function<E, DomainRepresentation> mapper) {
		this(mapAndSort(elements, mapper));
	}

	private static <E> SortedSet<DomainRepresentation> mapAndSort(Iterable<E> elements,
			Function<E, DomainRepresentation> mapper) {
		SortedSet<DomainRepresentation> result = new TreeSet<>();
		for (E e : elements)
			result.add(mapper.apply(e));
		return result;
	}

	public SetRepresentation(DomainRepresentation... elements) {
		this.elements = new TreeSet<>();
		for (DomainRepresentation repr : elements)
			this.elements.add(repr);
	}

	public SetRepresentation(Collection<DomainRepresentation> elements) {
		if (elements instanceof SortedSet)
			this.elements = (SortedSet<DomainRepresentation>) elements;
		else
			this.elements = new TreeSet<>(elements);
	}

	@Override
	public String toString() {
		return "[" + StringUtils.join(elements, ", ") + "]";
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
		SetRepresentation other = (SetRepresentation) obj;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}
}
