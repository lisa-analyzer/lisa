package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.SignatureCFG;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

/**
 * A interface unit of the program to analyze. A interface unit is a
 * {@link Unit} that only defines instance members, from which other units (both
 * {@link CompilationUnit} and {@link InterfaceUnit}) can inherit from
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InterfaceUnit extends Unit implements CodeElement {

	/**
	 * The location in the program of this interface unit
	 */
	private final CodeLocation location;

	/**
	 * The instance cfgs defined in this interface unit, indexed by
	 * {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, SignatureCFG> instanceSignatureCfgs;

	/**
	 * The instance cfgs defined in this interface unit, indexed by
	 * {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, ImplementedCFG> instanceImplementedCfgs;

	/**
	 * The collection of interface units this unit directly inherits from.
	 */
	private final Collection<InterfaceUnit> superInterfaceUnits;

	/**
	 * Whether or not the hierarchy of this interface unit has been fully
	 * computed, to avoid re-computation
	 */
	private boolean hierarchyComputed;

	public InterfaceUnit(CodeLocation location, String name) {
		super(name);
		this.location = location;
		instanceSignatureCfgs = new ConcurrentHashMap<>();
		instanceImplementedCfgs = new ConcurrentHashMap<>();
		superInterfaceUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		hierarchyComputed = false;
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

	public final Collection<SignatureCFG> getInstanceCFGs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> true, true, false, traverseHierarchy);
	}

	@SuppressWarnings("unchecked")
	private <T extends CodeMember> Collection<T> searchCodeMembers(Predicate<CodeMember> filter, boolean cfgs,
			boolean constructs, boolean traverseHierarchy) {
		Collection<T> result = new HashSet<>();

		if (cfgs) {
			for (SignatureCFG cfg : instanceSignatureCfgs.values())
				if (filter.test(cfg))
					result.add((T) cfg);

			for (ImplementedCFG cfg : instanceImplementedCfgs.values())
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
	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		super.validateAndFinalize();

		for (InterfaceUnit i : superInterfaceUnits)
			i.validateAndFinalize();

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

	@Override
	public CodeLocation getLocation() {
		return location;
	}
}
