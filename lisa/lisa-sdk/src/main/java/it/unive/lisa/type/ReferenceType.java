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

	private final ExternalSet<Type> innerTypes;

	public ReferenceType(ExternalSet<Type> innerTypes) {
		this.innerTypes = innerTypes;
	}

	public ReferenceType(Type t) {
		this(Caches.types().mkSingletonSet(t));
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

	/**
	 * Yields the inner types, that is, the types of the memory region that
	 * variables with this type point to.
	 * 
	 * @return the inner types
	 */
	public ExternalSet<Type> getInnerTypes() {
		return innerTypes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((innerTypes == null) ? 0 : innerTypes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ReferenceType))
			return false;
		ReferenceType other = (ReferenceType) obj;
		if (innerTypes == null) {
			if (other.innerTypes != null)
				return false;
		} else if (!innerTypes.equals(other.innerTypes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "*(" + StringUtils.join(innerTypes, ", ") + ")";
	}
}
