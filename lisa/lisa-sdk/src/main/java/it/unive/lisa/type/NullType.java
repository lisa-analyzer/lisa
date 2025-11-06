package it.unive.lisa.type;

import java.util.Collections;
import java.util.Set;

/**
 * The Null type, that is the type of {#link NullLiteral}. It implements the
 * singleton design pattern, that is the instances of this type are unique. The
 * unique instance of this type can be retrieved by {@link NullType#INSTANCE}.
 * It implements the singleton design pattern, that is the instances of this
 * type are unique. The unique instance of this type can be retrieved by
 * {@link NullType#INSTANCE}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class NullType
		implements
		InMemoryType {

	/**
	 * Unique instance of {@link NullType}.
	 */
	public static final NullType INSTANCE = new NullType();

	/**
	 * Builds the type. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected NullType() {
	}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public boolean equals(
			Object other) {
		return other instanceof NullType;
	}

	@Override
	public int hashCode() {
		return NullType.class.hashCode();
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return other.isInMemoryType() || other.isUntyped();
	}

	@Override
	public Type commonSupertype(
			Type other) {
		return other != null && other.isInMemoryType() ? other : Untyped.INSTANCE;
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

}
