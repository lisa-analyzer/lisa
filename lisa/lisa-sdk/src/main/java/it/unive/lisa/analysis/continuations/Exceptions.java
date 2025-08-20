package it.unive.lisa.analysis.continuations;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link Continuation} that represents an erroneous state caused by one or
 * more exceptions of a set of possible types being raised. This is usually used
 * to smash the states corresponding to exceptions that are either not
 * interesting or that are thrown several times for each analysis, causing a lot
 * of noise.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Exceptions
		extends
		Continuation {

	private final Set<Type> types;

	/**
	 * Builds a new continuation for the given exception type.
	 * 
	 * @param type the exception type
	 */
	public Exceptions(
			Type type) {
		this.types = Collections.singleton(type);
	}

	/**
	 * Builds a new continuation for the given set of exception types.
	 * 
	 * @param types the exception types
	 */
	public Exceptions(
			Set<Type> types) {
		this.types = Collections.unmodifiableSet(types);
	}

	/**
	 * Yields the exception types associated with this continuation.
	 * 
	 * @return the exception types
	 */
	public Set<Type> getTypes() {
		return types;
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * type added.
	 * 
	 * @param type the type to add
	 * 
	 * @return a new continuation with the added type
	 */
	public Exceptions add(
			Type type) {
		Set<Type> newTypes = new HashSet<>(types);
		newTypes.add(type);
		return new Exceptions(newTypes);
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * types added.
	 * 
	 * @param types the types to add
	 * 
	 * @return a new continuation with the added types
	 */
	public Exceptions addAll(
			Set<Type> types) {
		Set<Type> newTypes = new HashSet<>(this.types);
		newTypes.addAll(types);
		return new Exceptions(newTypes);
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * type removed.
	 * 
	 * @param type the type to remove
	 * 
	 * @return a new continuation with the removed type
	 */
	public Exceptions remove(
			Type type) {
		Set<Type> newTypes = new HashSet<>(types);
		newTypes.remove(type);
		return new Exceptions(newTypes);
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * types removed.
	 * 
	 * @param types the types to remove
	 * 
	 * @return a new continuation with the removed types
	 */
	public Exceptions removeAll(
			Set<Type> types) {
		Set<Type> newTypes = new HashSet<>(this.types);
		newTypes.removeAll(types);
		return new Exceptions(newTypes);
	}

	/**
	 * Yields whether or not the given type is within the exception types of
	 * this continuation.
	 * 
	 * @param type the type to check
	 * 
	 * @return whether or not the given type is within the exception types of
	 *             this continuation
	 */
	public boolean contains(
			Type type) {
		return types.contains(type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Exceptions other = (Exceptions) obj;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		return new SetRepresentation(types, StringRepresentation::new);
	}
}
