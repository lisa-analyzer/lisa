package it.unive.lisa.program;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public abstract class UnitWithSuperUnits extends Unit {

	/**
	 * The lazily computed collection of instances of this unit, that is, the
	 * collection of compilation units that directly or indirectly inherit from
	 * this unit
	 */
	protected final Collection<Unit> instances;
	
	protected UnitWithSuperUnits(String name) {
		super(name);
		instances = Collections.newSetFromMap(new ConcurrentHashMap<>());

	}
	
	/**
	 * Yields the collection of {@link CompilationUnit}s that are instances of
	 * this one, including itself. In other words, this method returns the
	 * collection of compilation units that directly or indirectly, inherit from
	 * this one.<br>
	 * <br>
	 * Note that this method returns an empty collection, until
	 * {@link #validateAndFinalize()} has been called.
	 * 
	 * @return the collection of units that are instances of this one, including
	 *             this unit itself
	 */
	public final Collection<Unit> getInstances() {
		return instances;
	}
	
	public abstract Collection<? extends UnitWithSuperUnits> getSuperUnits();

	/**
	 * Adds a new {@link UnitWithSuperUnits} as superunit of this unit.
	 * 
	 * @param unit the unit to add
	 * 
	 * @return {@code true} if the collection of superunits changed as a result
	 *             of the call
	 */
	public abstract boolean addSuperUnit(UnitWithSuperUnits unit);
}
