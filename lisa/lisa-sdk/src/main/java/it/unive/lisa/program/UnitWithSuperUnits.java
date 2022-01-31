package it.unive.lisa.program;

import java.util.Collection;

public abstract class UnitWithSuperUnits extends Unit {

	protected UnitWithSuperUnits(String name) {
		super(name);
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
