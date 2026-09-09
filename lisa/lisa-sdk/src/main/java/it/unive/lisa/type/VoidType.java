package it.unive.lisa.type;

import java.util.Collections;
import java.util.Set;

/**
 * The void type. It implements the singleton design pattern, that is the
 * instances of this type are unique. The unique instance of this type can be
 * retrieved by {@link VoidType#INSTANCE}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class VoidType
		implements
		Type {

	/**
	 * Unique instance of {@link VoidType}.
	 */
	public static final VoidType INSTANCE = new VoidType();

	/**
	 * Builds the type. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected VoidType() {
	}

	@Override
	public String toString() {
		return "void";
	}

	@Override
	public boolean equals(
			Object other) {
		return other instanceof VoidType;
	}

	@Override
	public int hashCode() {
		return VoidType.class.hashCode();
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return false;
	}

	@Override
	public Type commonSupertype(
			Type other) {
		// consistent with equals(): any VoidType instance, not just this
		// exact one, is considered the same type
		return other instanceof VoidType ? this : Untyped.INSTANCE;
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

}
