package it.unive.lisa.type;

import java.util.Collections;
import java.util.Set;
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
public class ReferenceType implements PointerType {

	private Set<Type> innerTypes;

	/**
	 * Builds the type for a reference to a location containing values of types
	 * {@code innerTypes}.
	 * 
	 * @param innerTypes the possible types of the referenced location
	 */
	public ReferenceType(Set<Type> innerTypes) {
		this.innerTypes = innerTypes;
	}

	/**
	 * Builds the type for a reference to a location containing values of types
	 * {@code t}.
	 * 
	 * @param t the type of the referenced location
	 */
	public ReferenceType(Type t) {
		this.innerTypes = Collections.singleton(t);
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
	public Set<Type> allInstances(TypeSystem types) {
		return Collections.singleton(this);
	}

	@Override
	public Set<Type> getInnerTypes() {
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
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
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
		if (innerTypes != null)
			if (innerTypes.size() == 1)
				return innerTypes.iterator().next() + "*";
			else
				return "[" + StringUtils.join(innerTypes, ", ") + "]*";
		return "?*";
	}
}
