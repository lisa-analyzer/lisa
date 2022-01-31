package it.unive.lisa.type;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * The type of type tokens, used as reference to types in code.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeTokenType implements Type {

	private final ExternalSet<Type> types;

	/**
	 * Builds the type token representing the given types.
	 * 
	 * @param types the types
	 */
	public TypeTokenType(ExternalSet<Type> types) {
		this.types = types;
	}

	/**
	 * Yields the {@link Type}s represented by this type token.
	 * 
	 * @return the types
	 */
	public ExternalSet<Type> getTypes() {
		return types;
	}

	@Override
	public String toString() {
		Set<String> sorted = new TreeSet<>();
		for (Type t : types)
			sorted.add(t.toString());
		return "token::" + sorted;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeTokenType other = (TypeTokenType) obj;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other instanceof TypeTokenType;
	}

	@Override
	public Type commonSupertype(Type other) {
		return other == this ? this : Untyped.INSTANCE;
	}

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
