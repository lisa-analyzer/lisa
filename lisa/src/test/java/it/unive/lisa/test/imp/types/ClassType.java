package it.unive.lisa.test.imp.types;

import it.unive.lisa.cfg.type.PointerType;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;
import java.util.HashMap;
import java.util.Map;

/**
 * A type representing an IMP class defined in an IMP program. ClassTypes are
 * instances of {@link PointerType}, have a name and a supertype. To ensure
 * uniqueness of ClassType objects, {@link #lookup(String, ClassType)} must be
 * used to retrieve existing instances (or automatically create one if no
 * matching instance exists).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ClassType implements PointerType {

	private static final Map<String, ClassType> types = new HashMap<>();

	/**
	 * Yields a unique instance (either an existing one or a fresh one) of
	 * {@link ClassType} representing a class with the given {@code name} and
	 * the given {@code supertype}.
	 * 
	 * @param name      the name of the class
	 * @param supertype the supertype of the class, or {@code null} if the class
	 *                      has no supertype.
	 * 
	 * @return the unique instance of {@link ClassType} representing the class
	 *             with the given name
	 */
	public static ClassType lookup(String name, ClassType supertype) {
		return types.computeIfAbsent(name, x -> new ClassType(name, supertype));
	}

	private final String name;

	private final ClassType supertype;

	private ClassType(String name, ClassType supertype) {
		this.name = name;
		this.supertype = supertype;
	}

	@Override
	public final boolean canBeAssignedTo(Type other) {
		return other instanceof ClassType && subclass((ClassType) other);
	}

	private boolean subclass(ClassType other) {
		return this == other || (supertype != null && supertype.subclass(other));
	}

	@Override
	public Type commonSupertype(Type other) {
		if (canBeAssignedTo(other))
			return other;

		if (other.canBeAssignedTo(this))
			return this;

		if (other.isNullType())
			return this;

		if (other.isArrayType())
			return Untyped.INSTANCE;

		return scanForSupertypeOf((ClassType) other);
	}

	private Type scanForSupertypeOf(ClassType other) {
		ClassType current = this;
		while (current != null) {
			if (other.canBeAssignedTo(current))
				return current;

			current = current.supertype;
		}

		return Untyped.INSTANCE;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((supertype == null) ? 0 : supertype.hashCode());
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
		ClassType other = (ClassType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (supertype == null) {
			if (other.supertype != null)
				return false;
		} else if (!supertype.equals(other.supertype))
			return false;
		return true;
	}
}
