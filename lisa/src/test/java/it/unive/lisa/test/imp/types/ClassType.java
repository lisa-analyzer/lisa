package it.unive.lisa.test.imp.types;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.type.PointerType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.workset.FIFOWorkingSet;
import it.unive.lisa.util.workset.WorkingSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A type representing an IMP class defined in an IMP program. ClassTypes are
 * instances of {@link PointerType} and {@link UnitType}, and are identified by
 * their name. To ensure uniqueness of ClassType objects,
 * {@link #lookup(String, CompilationUnit)} must be used to retrieve existing
 * instances (or automatically create one if no matching instance exists).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ClassType implements PointerType, UnitType {

	private static final Map<String, ClassType> types = new HashMap<>();

	/**
	 * Clears the cache of {@link ClassType}s created up to now.
	 */
	public static void clearAll() {
		types.clear();
	}

	/**
	 * Yields all the {@link ClassType}s defined up to now.
	 * 
	 * @return the collection of all the class types
	 */
	public static Collection<ClassType> all() {
		return types.values();
	}

	/**
	 * Yields a unique instance (either an existing one or a fresh one) of
	 * {@link ClassType} representing a class with the given {@code name},
	 * representing the given {@code unit}.
	 * 
	 * @param name the name of the class
	 * @param unit the unit underlying this type
	 * 
	 * @return the unique instance of {@link ClassType} representing the class
	 *             with the given name
	 */
	public static ClassType lookup(String name, CompilationUnit unit) {
		return types.computeIfAbsent(name, x -> new ClassType(name, unit));
	}

	private final String name;

	private final CompilationUnit unit;

	private ClassType(String name, CompilationUnit unit) {
		Objects.requireNonNull(name, "The name of a class type cannot be null");
		Objects.requireNonNull(unit, "The unit of a class type cannot be null");
		this.name = name;
		this.unit = unit;
	}

	@Override
	public CompilationUnit getUnit() {
		return unit;
	}

	@Override
	public final boolean canBeAssignedTo(Type other) {
		return other instanceof ClassType && subclass((ClassType) other);
	}

	private boolean subclass(ClassType other) {
		return this == other || unit.isInstanceOf(other.unit);
	}

	@Override
	public Type commonSupertype(Type other) {
		if (other.isNullType())
			return this;

		if (!other.isUnitType())
			return Untyped.INSTANCE;

		if (canBeAssignedTo(other))
			return other;

		if (other.canBeAssignedTo(this))
			return this;

		return scanForSupertypeOf((ClassType) other);
	}

	private Type scanForSupertypeOf(ClassType other) {
		WorkingSet<ClassType> ws = FIFOWorkingSet.mk();
		Set<ClassType> seen = new HashSet<>();
		ws.push(this);
		ClassType current;
		while (!ws.isEmpty()) {
			current = ws.pop();
			if (!seen.add(current))
				continue;

			if (other.canBeAssignedTo(current))
				return current;

			// null since we do not want to create new types here
			current.unit.getSuperUnits().forEach(u -> ws.push(lookup(u.getName(), null)));
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
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
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
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		return true;
	}

	@Override
	public Collection<Type> allInstances() {
		Collection<Type> instances = new HashSet<>();
		for (CompilationUnit in : unit.getInstances())
			instances.add(lookup(in.getName(), null));
		return instances;
	}
}
