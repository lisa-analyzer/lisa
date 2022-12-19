package it.unive.lisa.imp.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Unit;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;

/**
 * A type representing an IMP interface defined in an IMP program. Interface
 * type are instances of {@link UnitType}, and are identified by their name. To
 * ensure uniqueness of InterfaceType objects,
 * {@link #lookup(String, InterfaceUnit)} must be used to retrieve existing
 * instances (or automatically create one if no matching instance exists).
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public final class InterfaceType implements UnitType {

	private static final Map<String, InterfaceType> types = new HashMap<>();

	/**
	 * Clears the cache of {@link InterfaceType}s created up to now.
	 */
	public static void clearAll() {
		types.clear();
	}

	/**
	 * Yields all the {@link InterfaceType}s defined up to now.
	 * 
	 * @return the collection of all the interface types
	 */
	public static Collection<InterfaceType> all() {
		return types.values();
	}

	/**
	 * Yields a unique instance (either an existing one or a fresh one) of
	 * {@link InterfaceType} representing an interface with the given
	 * {@code name}, representing the given {@code unit}.
	 * 
	 * @param name the name of the interface
	 * @param unit the unit underlying this type
	 * 
	 * @return the unique instance of {@link InterfaceType} representing the
	 *             interface with the given name
	 */
	public static InterfaceType lookup(String name, InterfaceUnit unit) {
		return types.computeIfAbsent(name, x -> new InterfaceType(name, unit));
	}

	private final String name;

	private final InterfaceUnit unit;

	private InterfaceType(String name, InterfaceUnit unit) {
		Objects.requireNonNull(name, "The name of an interface type cannot be null");
		Objects.requireNonNull(unit, "The unit of a interface type cannot be null");
		this.name = name;
		this.unit = unit;
	}

	@Override
	public InterfaceUnit getUnit() {
		return unit;
	}

	@Override
	public final boolean canBeAssignedTo(Type other) {
		if (other instanceof InterfaceType)
			return subclass((InterfaceType) other);

		return false;
	}

	private boolean subclass(InterfaceType other) {
		return this == other || unit.isInstanceOf(other.unit);
	}

	@Override
	public Type commonSupertype(Type other) {
		if (other.isNullType())
			return this;

		if (!other.isUnitType())
			return Untyped.INSTANCE;

		if (other.canBeAssignedTo(this))
			return this;

		return scanForSupertypeOf((UnitType) other);
	}

	private Type scanForSupertypeOf(UnitType other) {
		WorkingSet<InterfaceType> ws = FIFOWorkingSet.mk();
		Set<InterfaceType> seen = new HashSet<>();
		ws.push(this);
		InterfaceType current;
		while (!ws.isEmpty()) {
			current = ws.pop();
			if (!seen.add(current))
				continue;

			if (other.canBeAssignedTo(current))
				return current;

			// null since we do not want to create new types here
			current.unit.getImmediateAncestors().forEach(u -> ws.push(lookup(u.getName(), null)));
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
		InterfaceType other = (InterfaceType) obj;
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
	public Set<Type> allInstances(TypeSystem types) {
		Set<Type> instances = new HashSet<>();
		for (Unit un : unit.getInstances())
			if (un instanceof InterfaceUnit)
				instances.add(lookup(un.getName(), null));
			else
				instances.add(ClassType.lookup(un.getName(), null));
		return instances;
	}
}
