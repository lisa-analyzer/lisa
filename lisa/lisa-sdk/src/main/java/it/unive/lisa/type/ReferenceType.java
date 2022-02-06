package it.unive.lisa.type;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

/**
 * A type for references to memory regions. This type is the one of variables
 * holding references to entities that leave in the heap. For instance, where
 * creating an array if {@code int32}, the location in memory containing the
 * array will have type {@code int32[]}, while all variables referencing that
 * location will have type {@code referenceType(int32[])}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ReferenceType implements PointerType {

	private ExternalSet<Type> innerTypes;

	// TODO the purpose of this field is to avoid using the cache
	// when defining the descriptors of cfgs, we have to find another
	// workaround to this problem
	private Type innerType;

	/**
	 * Builds the type for a reference to a location containing values of types
	 * {@code innerTypes}.
	 * 
	 * @param innerTypes the possible types of the referenced location
	 */
	public ReferenceType(ExternalSet<Type> innerTypes) {
		this.innerTypes = innerTypes;
	}

	/**
	 * Builds the type for a reference to a location containing values of types
	 * {@code t}.
	 * 
	 * @param t the type of the referenced location
	 */
	public ReferenceType(Type t) {
		innerType = t;
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other instanceof PointerType;
	}

	@Override
	public Type commonSupertype(Type other) {
		return equals(other) ? this : Untyped.INSTANCE;
	}

	@Override
	public Collection<Type> allInstances() {
		return Caches.types().mkSingletonSet(this);
	}

	@Override
	public ExternalSet<Type> getInnerTypes() {
		return innerTypes != null ? innerTypes : Caches.types().mkSingletonSet(innerType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if (innerTypes != null)
			if (innerTypes.size() == 1)
				result = prime * result + innerTypes.first().hashCode();
			else
				result = prime * result + ((innerTypes == null) ? 0 : innerTypes.hashCode());
		else
			result = prime * result + ((innerType == null) ? 0 : innerType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ReferenceType))
			return false;
		ReferenceType other = (ReferenceType) obj;

		if (innerTypes != null) {
			if (innerTypes.size() == 1) {
				if (other.innerTypes != null)
					if (other.innerTypes.size() != 1)
						// [x] != [y, z]
						return false;
					else
						// [x] ?= [y]
						return innerTypes.first().equals(other.innerTypes.first());
				else
					// [x] ?= y
					return innerTypes.first().equals(other.innerType);
			} else {
				if (other.innerTypes != null)
					if (other.innerTypes.size() != 1)
						// [x, w] ?= [y, z]
						return innerTypes.equals(other.innerTypes);
					else
						// [x, w] != [y]
						return false;
				else
					// [x, w] != y
					return false;
			}
		} else if (innerType != null) {
			if (other.innerTypes != null)
				if (other.innerTypes.size() != 1)
					// x != [y, z]
					return false;
				else
					// x ?= [y]
					return innerType.equals(other.innerTypes.first());
			else
				// x ?= y
				return innerType.equals(other.innerType);
		} else
			return false;
	}

	@Override
	public String toString() {
		if (innerTypes != null)
			if (innerTypes.size() == 1)
				return innerTypes.first() + "*";
			else
				return "[" + StringUtils.join(innerTypes, ", ") + "]*";
		return innerType + "*";
	}
}
