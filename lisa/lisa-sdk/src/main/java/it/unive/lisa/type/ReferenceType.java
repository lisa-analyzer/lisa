package it.unive.lisa.type;

import java.util.HashSet;
import java.util.Set;

/**
 * A type for references to memory regions. This type is the one of variables
 * holding references to entities that leave in the heap. For instance, where
 * creating an array if {@code int32}, the location in memory containing the
 * array will have type {@code int32[]}, while all variables referencing that
 * location will have type {@code referenceType(int32[])}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ReferenceType implements PointerType {

	private final Type innerType;

	/**
	 * Builds the type for a reference to a location containing values of types
	 * {@code t}.
	 * 
	 * @param t the type of the referenced location
	 */
	public ReferenceType(
			Type t) {
		this.innerType = t;
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return other instanceof ReferenceType && getInnerType().canBeAssignedTo(other.asReferenceType().getInnerType())
				|| other.isUntyped();
	}

	@Override
	public Type commonSupertype(
			Type other) {
		if (equals(other))
			return this;
		else if (other instanceof ReferenceType)
			return new ReferenceType(getInnerType().commonSupertype(other.asReferenceType().getInnerType()));

		return Untyped.INSTANCE;
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		Set<Type> instances = new HashSet<>();
		for (Type inner : getInnerType().allInstances(types))
			instances.add(new ReferenceType(inner));
		instances.add(this);
		return instances;
	}

	@Override
	public Type getInnerType() {
		return innerType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((innerType == null) ? 0 : innerType.hashCode());
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
		ReferenceType other = (ReferenceType) obj;
		if (innerType == null) {
			if (other.innerType != null)
				return false;
		} else if (!innerType.equals(other.innerType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return innerType + "*";
	}
}
