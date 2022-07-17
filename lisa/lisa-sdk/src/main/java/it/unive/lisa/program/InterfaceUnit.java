package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.SignatureCFG;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

/**
 * A interface unit of the program to analyze. A interface unit is a
 * {@link Unit} that only defines instance members, from which other units (both
 * {@link CompilationUnit} and {@link InterfaceUnit}) can inherit from
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class InterfaceUnit extends UnitWithSuperUnits implements CodeElement {

	/**
	 * The instance globals defined in this unit, indexed by
	 * {@link Global#getName()}
	 */
	private final Map<String, Global> instanceGlobals;

	/**
	 * The location in the program of this interface unit
	 */
	private final CodeLocation location;

	/**
	 * The instance cfgs defined in this interface unit, indexed by
	 * {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, CFG> instanceCFG;

	/**
	 * The collection of interface units this unit directly inherits from.
	 */
	private final Collection<InterfaceUnit> superInterfaceUnits;

	/**
	 * Whether or not the hierarchy of this interface unit has been fully
	 * computed, to avoid re-computation
	 */
	private boolean hierarchyComputed;

	/**
	 * Builds an interface unit, defined at the given location.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param name     the name of the unit
	 */
	public InterfaceUnit(CodeLocation location, String name) {
		super(name);
		this.location = location;
		instanceCFG = new ConcurrentHashMap<>();
		instanceGlobals = new ConcurrentHashMap<>();
		superInterfaceUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		hierarchyComputed = false;
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

	/**
	 * Yields the collection of instance {@link SignatureCFG}s defined in this
	 * unit. Each cfg is uniquely identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), meaning that there are no two
	 * signature cfgs having the same signature in each unit. Signature cfgs
	 * must be overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns signature cfgs
	 *                              from superunits, transitively
	 * 
	 * @return the collection of signature cfgs
	 */
	public final Collection<SignatureCFG> getSignatureCFGs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof SignatureCFG, true, false, traverseHierarchy);
	}

	/**
	 * Yields the collection of instance {@link ImplementedCFG}s defined in this
	 * unit. Each cfg is uniquely identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), meaning that there are no two
	 * instance cfgs having the same signature in each unit. Instance cfgs can
	 * be overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns instance cfgs from
	 *                              superunits, transitively
	 * 
	 * @return the collection of instance cfgs
	 */
	public final Collection<ImplementedCFG> getImplementedCFGs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof ImplementedCFG, true, false, traverseHierarchy);
	}

	/**
	 * Yields the collection of instance {@link CFG}s defined in this unit. Each
	 * cfg is uniquely identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), meaning that there are no two
	 * instance cfgs having the same signature in each unit. Instance cfgs can
	 * be overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns cfgs from
	 *                              superunits, transitively
	 * 
	 * @return the collection of cfgs
	 */
	public final Collection<ImplementedCFG> getInstanceCFGs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof CFG, true, false, traverseHierarchy);
	}

	@SuppressWarnings("unchecked")
	private <T extends CodeMember> Collection<T> searchCodeMembers(Predicate<CodeMember> filter, boolean cfgs,
			boolean constructs, boolean traverseHierarchy) {
		Collection<T> result = new HashSet<>();

		if (cfgs) {
			for (CFG cfg : instanceCFG.values())
				if (filter.test(cfg))
					result.add((T) cfg);
		}

		if (!traverseHierarchy)
			return result;

		for (InterfaceUnit cu : superInterfaceUnits)
			for (CodeMember sup : cu.searchCodeMembers(filter, cfgs, constructs, true))
				if (!result.stream().anyMatch(cfg -> sup.getDescriptor().overriddenBy().contains(cfg)))
					// we skip the ones that are overridden by code members that
					// are already in the set, since they are "hidden" from the
					// point of view of this unit
					result.add((T) sup);

		return result;
	}

	/**
	 * Finds all the instance code members whose signature matches the one of
	 * the given {@link CFGDescriptor}, according to
	 * {@link CFGDescriptor#matchesSignature(CFGDescriptor)}.
	 * 
	 * @param signature         the descriptor providing the signature to match
	 * @param traverseHierarchy if {@code true}, also returns instance code
	 *                              members from superunits, transitively
	 * 
	 * @return the collection of instance code members that match the given
	 *             signature
	 */
	public final Collection<CodeMember> getMatchingInstanceCodeMembers(CFGDescriptor signature,
			boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm.getDescriptor().matchesSignature(signature), true, true, traverseHierarchy);
	}

	@Override
	public final boolean addSuperUnit(UnitWithSuperUnits unit) {
		return superInterfaceUnits.add((InterfaceUnit) unit);
	}

	@Override
	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		super.validateAndFinalize();

		for (InterfaceUnit i : superInterfaceUnits)
			i.validateAndFinalize();

		addInstance(this);

		for (InterfaceUnit s : superInterfaceUnits)
			for (CodeMember sup : s.getInstanceCFGs(true)) {
				Collection<CodeMember> implementing = getMatchingInstanceCodeMembers(sup.getDescriptor(), false);
				if (implementing.size() > 1)
					throw new ProgramValidationException(
							sup.getDescriptor().getSignature() + " is implemented multiple times in unit " + this + ": "
									+ StringUtils.join(", ", implementing));
				else if (implementing.size() == 1) {
					CodeMember over = implementing.iterator().next();
					over.getDescriptor().overrides().addAll(sup.getDescriptor().overrides());
					over.getDescriptor().overrides().add(sup);
					over.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(over));
				}
			}

		hierarchyComputed = true;
	}

	/**
	 * Adds a new instance {@link CFG}, identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), to this unit. Cfgs can be
	 * overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param cfg the cfg to add
	 * 
	 * @return {@code true} if there was no cfg previously associated with the
	 *             same signature, {@code false} otherwise. If this method
	 *             returns {@code false}, the given cfg is discarded.
	 */
	public final boolean addInstanceCFG(CFG cfg) {
		return instanceCFG.putIfAbsent(cfg.getDescriptor().getSignature(), cfg) == null;
	}

	@Override
	public final Collection<InterfaceUnit> getSuperUnits() {
		return superInterfaceUnits;
	}

	/**
	 * Adds an instance unit to this interface unit.
	 * 
	 * @param unit the unit to be added
	 * 
	 * @throws ProgramValidationException if this interface unit already
	 *                                        contains the unit to be added
	 */
	protected final void addInstance(Unit unit) throws ProgramValidationException {
		if (superInterfaceUnits.contains(unit))
			throw new ProgramValidationException("Found loop in compilation units hierarchy: " + unit
					+ " is both a super unit and an instance of " + this);
		instances.add(unit);

		for (InterfaceUnit sup : superInterfaceUnits)
			sup.addInstance(unit);
	}

	@Override
	public final boolean isInstanceOf(UnitWithSuperUnits unit) {
		return this == unit || (hierarchyComputed ? unit.instances.contains(this)
				: superInterfaceUnits.stream().anyMatch(u -> u.isInstanceOf(unit)));
	}

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	/**
	 * Yields the collection of instance {@link Global}s defined in this unit.
	 * Each global is uniquely identified by its name, meaning that there are no
	 * two instance globals having the same name in each unit.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns instance globals
	 *                              from superunits, transitively
	 * 
	 * @return the collection of instance globals
	 */
	public final Collection<Global> getInstanceGlobals(boolean traverseHierarchy) {
		return searchGlobals(g -> true, traverseHierarchy);
	}

	private Collection<Global> searchGlobals(Predicate<Global> filter, boolean traverseHierarchy) {
		Map<String, Global> result = new HashMap<>();
		for (Global g : instanceGlobals.values())
			if (filter.test(g))
				result.put(g.getName(), g);

		if (!traverseHierarchy)
			return result.values();

		for (InterfaceUnit cu : superInterfaceUnits)
			for (Global sup : cu.searchGlobals(filter, true))
				if (!result.containsKey(sup.getName()))
					// we skip the ones that are hidden by globals that
					// are already in the set, since they are "hidden" from the
					// point of view of this unit
					result.put(sup.getName(), sup);

		return result.values();
	}

	@Override
	public Collection<CodeMember> getInstanceCodeMembers(boolean traverseHierarchy) {
		Set<CodeMember> all = new HashSet<>(getInstanceCFGs(traverseHierarchy));
		all.addAll(getSignatureCFGs(traverseHierarchy));
		return all;
	}
}
