package it.unive.lisa.analysis.representation;

import it.unive.lisa.util.collections.CollectionUtilities;

public abstract class DomainRepresentation implements Comparable<DomainRepresentation> {

	@Override
	public final int compareTo(DomainRepresentation o) {
		if (o == null)
			return 1;

		if (getClass() != o.getClass())
			return getClass().getName().compareTo(o.getClass().getName());

		return CollectionUtilities.nullSafeCompare(true, toString(), o.toString(), String::compareTo);
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract String toString();
}
