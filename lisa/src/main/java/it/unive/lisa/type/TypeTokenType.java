package it.unive.lisa.type;

import java.util.Collection;
import java.util.Collections;

import it.unive.lisa.util.collections.ExternalSet;

/**
 * The type of type tokens, used as reference to types in code.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeTokenType implements Type {

	private final ExternalSet<Type> types;

	public TypeTokenType(ExternalSet<Type> types) {
		this.types = types;
	}

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
