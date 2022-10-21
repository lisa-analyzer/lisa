package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CodeLocation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A interface unit of the program to analyze. A interface unit is a
 * {@link Unit} that only defines instance members, from which other units (both
 * {@link ClassUnit} and {@link InterfaceUnit}) can inherit from
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class InterfaceUnit extends CompilationUnit {

	/**
	 * The collection of interface units this unit directly inherits from.
	 */
	private final List<InterfaceUnit> superinterfaces;

	/**
	 * Builds an interface unit, defined at the given location.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param name     the name of the unit
	 * @param sealed   whether or not this unit can be inherited from
	 */
	public InterfaceUnit(CodeLocation location, String name, boolean sealed) {
		super(location, name, sealed);
		superinterfaces = new LinkedList<>();
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

	/**
	 * Adds the given {@link InterfaceUnit} to the list of direct ancestors of
	 * this interface.
	 * 
	 * @param unit the unit to add
	 * 
	 * @return {@code true} only if the list has changed
	 */
	public boolean addSuperinterface(InterfaceUnit unit) {
		return superinterfaces.add(unit);
	}

	@Override
	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		addInstance(this);
		if (!getInstanceGlobals(false).isEmpty())
			throw new ProgramValidationException(this + " is an interface and cannot have instange globals.");
		super.validateAndFinalize();
	}

	@Override
	public Collection<CompilationUnit> getImmediateAncestors() {
		return superinterfaces.stream().map(CompilationUnit.class::cast).collect(Collectors.toList());
	}

	/**
	 * Adds an instance unit to this interface unit.
	 * 
	 * @param unit the unit to be added
	 * 
	 * @throws ProgramValidationException if this interface unit already
	 *                                        contains the unit to be added
	 */
	void addInstance(Unit unit) throws ProgramValidationException {
		if (superinterfaces.contains(unit))
			throw new ProgramValidationException("Found loop in compilation units hierarchy: " + unit
					+ " is both a super unit and an instance of " + this);
		instances.add(unit);

		for (InterfaceUnit sup : superinterfaces)
			sup.addInstance(unit);
	}

	@Override
	public boolean isInstanceOf(CompilationUnit unit) {
		return this == unit || (hierarchyComputed ? unit.instances.contains(this)
				: superinterfaces.stream().anyMatch(u -> u.isInstanceOf(unit)));
	}

	@Override
	public boolean addAncestor(CompilationUnit unit) {
		if (unit instanceof InterfaceUnit)
			return superinterfaces.add((InterfaceUnit) unit);
		else
			return false;
	}
}
