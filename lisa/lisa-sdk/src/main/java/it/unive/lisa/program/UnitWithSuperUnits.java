package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeMember;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An unit of the program to analyze with super unit.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">VincenzoArceri</a>
 */
public abstract class UnitWithSuperUnits extends Unit {

	/**
	 * The lazily computed collection of instances of this unit, that is, the
	 * collection of compilation units that directly or indirectly inherit from
	 * this unit.
	 */
	protected final Collection<Unit> instances;

	/**
	 * Builds an unit with super unit.
	 * 
	 * @param name the name of the unit
	 */
	protected UnitWithSuperUnits(String name) {
		super(name);
		instances = Collections.newSetFromMap(new ConcurrentHashMap<>());

	}

	/**
	 * Yields the collection of instance {@link CodeMember}s defined in this
	 * unit.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns instance code
	 *                              members from superunits, transitively
	 * 
	 * @return the collection of instance code members
	 */
	public abstract Collection<CodeMember> getInstanceCodeMembers(boolean traverseHierarchy);

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

	/**
	 * Yields the collection of {@link UnitWithSuperUnits}s that are superunit
	 * of this one, including itself.
	 * 
	 * @return the collection of units that are superunits of this one,
	 *             including this unit itself
	 */
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

	/**
	 * Yields {@code true} if and only if this unit is an instance of the given
	 * one. This method works correctly even if {@link #validateAndFinalize()}
	 * has not been called yet, and thus the if collection of instances of the
	 * given unit is not yet available.
	 * 
	 * @param unit the other unit
	 * 
	 * @return {@code true} only if that condition holds
	 */
	public abstract boolean isInstanceOf(UnitWithSuperUnits unit);
}
