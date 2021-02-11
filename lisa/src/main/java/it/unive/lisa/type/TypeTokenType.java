package it.unive.lisa.type;

import it.unive.lisa.util.collections.ExternalSet;
import java.util.Collection;
import java.util.Collections;

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
		return types.toString();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TypeTokenType && types.equals(((TypeTokenType) other).types);
	}

	@Override
	public int hashCode() {
		return types.hashCode();
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
