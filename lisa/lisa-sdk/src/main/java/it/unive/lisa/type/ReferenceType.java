package it.unive.lisa.type;

import java.util.Collection;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class ReferenceType implements PointerType {

	private final ExternalSet<Type> innerTypes;

	public ReferenceType(ExternalSet<Type> innerTypes) {
		this.innerTypes = innerTypes;
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
		return "*(" + innerTypes + ")";
	}
}
