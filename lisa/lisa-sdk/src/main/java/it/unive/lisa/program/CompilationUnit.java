package it.unive.lisa.program;

import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.ICFG;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.AbstractCFG;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

/**
 * A compilation unit of the program to analyze. A compilation unit is a
 * {@link Unit} that also defines instance members, that can be inherited by
 * subunits.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CompilationUnit extends UnitWithSuperUnits implements CodeElement {

	/**
	 * The location in the program of this unit
	 */
	private final CodeLocation location;

	/**
	 * The collection of compilation units this unit directly inherits from.
	 * This collection may contain both concrete and abstract compilation unit.
	 */
	private final Collection<CompilationUnit> superCompilationUnits;

	/**
	 * The collection of interface units this unit directly inherits from.
	 */
	private final Collection<InterfaceUnit> superInterfaceUnits;

	/**
	 * The instance globals defined in this unit, indexed by
	 * {@link Global#getName()}
	 */
	private final Map<String, Global> instanceGlobals;

	/**
	 * The instance cfgs defined in this unit, indexed by
	 * {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, CFG> instanceCfgs;

	/**
	 * The instance cfgs defined in this unit, indexed by
	 * {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, AbstractCFG> signatureCfgs;

	/**
	 * The instance constructs ({@link NativeCFG}s) defined in this unit,
	 * indexed by {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, NativeCFG> instanceConstructs;

	/**
	 * Whether or not this compilation unit is sealed, meaning that it cannot be
	 * used as super unit of other compilation units
	 */
	private final boolean sealed;

	/**
	 * Whether or not the hierarchy of this compilation unit has been fully
	 * computed, to avoid re-computation
	 */
	private boolean hierarchyComputed;

	private Annotations annotations;

	/**
	 * Whether or not this compilation unit is abstract
	 */
	private final boolean abstractUnit;

	/**
	 * Builds a concrete compilation unit, defined at the given program point.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param name     the name of the unit
	 * @param sealed   whether or not this unit is sealed, meaning that it
	 *                     cannot be used as super unit of other compilation
	 *                     units
	 */
	public CompilationUnit(CodeLocation location, String name, boolean sealed) {
		this(location, name, sealed, false);
	}

	/**
	 * Builds a compilation unit, defined at the given program point.
	 * 
	 * @param location     the location where the unit is define within the
	 *                         source file
	 * @param name         the name of the unit
	 * @param sealed       whether or not this unit is sealed, meaning that it
	 *                         cannot be used as super unit of other compilation
	 *                         units
	 * @param abstractUnit whether or not this compilation unit is abstract
	 */
	public CompilationUnit(CodeLocation location, String name, boolean sealed, boolean abstractUnit) {
		super(name);
		Objects.requireNonNull(location, "The location of a unit cannot be null.");
		this.location = location;
		this.sealed = sealed;
		superCompilationUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		superInterfaceUnits = Collections.newSetFromMap(new ConcurrentHashMap<>());
		instanceGlobals = new ConcurrentHashMap<>();
		instanceCfgs = new ConcurrentHashMap<>();
		signatureCfgs = new ConcurrentHashMap<>();
		instanceConstructs = new ConcurrentHashMap<>();
		hierarchyComputed = false;
		annotations = new Annotations();
		this.abstractUnit = abstractUnit;
	}

	/**
	 * Yields whether or not this unit is sealed, meaning that it cannot be used
	 * as super unit of other compilation units.
	 * 
	 * @return {@code true} if this unit is sealed
	 */
	public final boolean isSealed() {
		return sealed;
	}

	/**
	 * Yields the collection of {@link CompilationUnit}s that this unit
	 * <i>directly</i> inherits from. The returned collection does not include
	 * units that are transitively inherited.
	 * 
	 * @return the collection of direct super units
	 */
	@Override
	public final Collection<CompilationUnit> getSuperUnits() {
		// TODO: it should return both super compilation units and super
		// interfaces
		return superCompilationUnits;
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

	/**
	 * Yields the collection of instance {@link CFG}s defined in this
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
	public final Collection<CFG> getInstanceCFGs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof CFG, true, false, traverseHierarchy);
	}

	/**
	 * Yields the collection of instance {@link AbstractCFG}s defined in this
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
	public final Collection<AbstractCFG> getSignatureCFGs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof AbstractCFG, true, false, traverseHierarchy);
	}

	/**
	 * Yields the collection of instance constructs ({@link NativeCFG}s) defined
	 * in this unit. Each construct is uniquely identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), meaning that there are no two
	 * instance constructs having the same signature in each unit. Instance
	 * constructs can be overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns instance
	 *                              constructs from superunits, transitively
	 * 
	 * @return the collection of instance constructs
	 */
	public final Collection<NativeCFG> getInstanceConstructs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> true, false, true, traverseHierarchy);
	}

	@Override
	public final boolean addSuperUnit(UnitWithSuperUnits unit) {
		if (unit instanceof CompilationUnit)
			return superCompilationUnits.add((CompilationUnit) unit);
		else
			return superInterfaceUnits.add((InterfaceUnit) unit);
	}

	/**
	 * Adds a new instance {@link Global}, identified by its name
	 * ({@link Global#getName()}), to this unit.
	 * 
	 * @param global the global to add
	 * 
	 * @return {@code true} if there was no instance global previously
	 *             associated with the same name, {@code false} otherwise. If
	 *             this method returns {@code false}, the given global is
	 *             discarded.
	 */
	public final boolean addInstanceGlobal(Global global) {
		return instanceGlobals.putIfAbsent(global.getName(), global) == null;
	}

	/**
	 * Adds a new instance {@link CFG}, identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), to this unit. Instance cfgs can
	 * be overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param cfg the cfg to add
	 * 
	 * @return {@code true} if there was no instance cfg previously associated
	 *             with the same signature, {@code false} otherwise. If this
	 *             method returns {@code false}, the given cfg is discarded.
	 */
	public final boolean addInstanceCFG(CFG cfg) {
		CFG c = instanceCfgs.putIfAbsent(cfg.getDescriptor().getSignature(), cfg);
		if (sealed)
			if (c == null)
				cfg.getDescriptor().setOverridable(false);
			else
				c.getDescriptor().setOverridable(false);
		return c == null;
	}

	/**
	 * Adds a new instance {@link NativeCFG}, identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), to this unit. Instance constructs
	 * can be overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param construct the construct to add
	 * 
	 * @return {@code true} if there was no instance construct previously
	 *             associated with the same signature, {@code false} otherwise.
	 *             If this method returns {@code false}, the given construct is
	 *             discarded.
	 */
	public final boolean addInstanceConstruct(NativeCFG construct) {
		NativeCFG c = instanceConstructs.putIfAbsent(construct.getDescriptor().getSignature(), construct);
		if (sealed)
			if (c == null)
				construct.getDescriptor().setOverridable(false);
			else
				c.getDescriptor().setOverridable(false);
		return c == null;
	}

	/**
	 * Yields the instance {@link CFG} defined in this unit having
	 * the given signature ({@link CFGDescriptor#getSignature()}), if any.
	 * 
	 * @param signature         the signature of the cfg to find
	 * @param traverseHierarchy if {@code true}, also returns instance cfgs from
	 *                              superunits, transitively
	 * 
	 * @return the instance cfg with the given signature, or {@code null}
	 */
	public final CFG getInstanceCFG(String signature, boolean traverseHierarchy) {
		Collection<
				CFG> res = searchCodeMembers(
						cm -> cm instanceof CFG && cm.getDescriptor().getSignature().equals(signature), true,
						false, traverseHierarchy);
		if (res.isEmpty())
			return null;
		return res.stream().findFirst().get();
	}

	/**
	 * Yields the instance {@link NativeCFG} defined in this unit having the
	 * given signature ({@link CFGDescriptor#getSignature()}), if any.
	 * 
	 * @param signature         the signature of the construct to find
	 * @param traverseHierarchy if {@code true}, also returns instance
	 *                              constructs from superunits, transitively
	 * 
	 * @return the instance construct with the given signature, or {@code null}
	 */
	public final NativeCFG getInstanceConstruct(String signature, boolean traverseHierarchy) {
		Collection<NativeCFG> res = searchCodeMembers(cm -> cm.getDescriptor().getSignature().equals(signature), false,
				true, traverseHierarchy);
		if (res.isEmpty())
			return null;
		return res.stream().findFirst().get();
	}

	/**
	 * Yields the instance {@link Global} defined in this unit having the given
	 * name ({@link Global#getName()}), if any.
	 * 
	 * @param name              the name of the global to find
	 * @param traverseHierarchy if {@code true}, also returns instance globals
	 *                              from superunits, transitively
	 * 
	 * @return the instance global with the given name, or {@code null}
	 */
	@Override
	public final Global getInstanceGlobal(String name, boolean traverseHierarchy) {
		Collection<Global> res = searchGlobals(cm -> cm.getName().equals(name), traverseHierarchy);
		if (res.isEmpty())
			return null;
		return res.stream().findFirst().get();
	}

	/**
	 * Yields the instance {@link CodeMember} defined in this unit having the
	 * given signature ({@link CFGDescriptor#getSignature()}), if any. This
	 * method searches the code member both among the instance cfgs and instance
	 * constructs defined in this unit.
	 * 
	 * @param signature         the signature of the code member to find
	 * @param traverseHierarchy if {@code true}, also returns instance code
	 *                              members from superunits, transitively
	 * 
	 * @return the instance code member with the given signature, or
	 *             {@code null}
	 */
	public final CodeMember getInstanceCodeMember(String signature, boolean traverseHierarchy) {
		Collection<CodeMember> res = searchCodeMembers(cm -> cm.getDescriptor().getSignature().equals(signature), true,
				true, traverseHierarchy);
		if (res.isEmpty())
			return null;
		return res.stream().findFirst().get();
	}

	/**
	 * Yields the collection of all instance {@link CFG}s defined in
	 * this unit that have the given name.
	 * 
	 * @param name              the name of the constructs to include
	 * @param traverseHierarchy if {@code true}, also returns instance cfgs from
	 *                              superunits, transitively
	 * 
	 * @return the collection of instance cfgs with the given name
	 */
	public final Collection<CFG> getInstanceCFGsByName(String name, boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof CFG && cm.getDescriptor().getName().equals(name), true,
				false, traverseHierarchy);
	}

	/**
	 * Yields the collection of all instance {@link NativeCFG}s defined in this
	 * unit that have the given name.
	 * 
	 * @param name              the name of the constructs to include
	 * @param traverseHierarchy if {@code true}, also returns instance
	 *                              constructs from superunits, transitively
	 * 
	 * @return the collection of instance constructs with the given name
	 */
	public final Collection<NativeCFG> getInstanceConstructsByName(String name, boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm.getDescriptor().getName().equals(name), false, true, traverseHierarchy);
	}

	/**
	 * Yields the collection of all instance {@link CodeMember}s defined in this
	 * unit that have the given name. This method searches the code member both
	 * among the instance cfgs and instance constructs defined in this unit.
	 * 
	 * @param name              the name of the code members to include
	 * @param traverseHierarchy if {@code true}, also returns instance code
	 *                              members from superunits, transitively
	 * 
	 * @return the collection of code members with the given name
	 */
	public final Collection<CodeMember> getInstanceCodeMembersByName(String name, boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm.getDescriptor().getName().equals(name), true, true, traverseHierarchy);
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

	/**
	 * Searches among instance code members, returning a collection containing
	 * all members that satisfy the given condition.
	 * 
	 * @param <T>               the concrete type of elements that this method
	 *                              returns
	 * @param filter            the filtering condition to use for selecting
	 *                              which code members to return
	 * @param cfgs              if {@code true}, the search will include
	 *                              instance cfgs
	 * @param constructs        if {@code true}, the search will include
	 *                              instance constructs
	 * @param traverseHierarchy if {@code true}, also returns instance code
	 *                              members from superunits, transitively
	 * 
	 * @return the collection of matching code members
	 */
	@SuppressWarnings("unchecked")
	private <T extends CodeMember> Collection<T> searchCodeMembers(Predicate<CodeMember> filter, boolean cfgs,
			boolean constructs, boolean traverseHierarchy) {
		Collection<T> result = new HashSet<>();

		if (cfgs) {
			for (CFG cfg : instanceCfgs.values())
				if (filter.test(cfg))
					result.add((T) cfg);

			for (AbstractCFG cfg : signatureCfgs.values())
				if (filter.test(cfg))
					result.add((T) cfg);
		}

		if (constructs)
			for (NativeCFG construct : instanceConstructs.values())
				if (filter.test(construct))
					result.add((T) construct);

		if (!traverseHierarchy)
			return result;

		for (CompilationUnit cu : superCompilationUnits)
			for (CodeMember sup : cu.searchCodeMembers(filter, cfgs, constructs, true))
				if (!result.stream().anyMatch(cfg -> sup.getDescriptor().overriddenBy().contains(cfg)))
					// we skip the ones that are overridden by code members that
					// are already in the set, since they are "hidden" from the
					// point of view of this unit
					result.add((T) sup);

		return result;
	}

	/**
	 * Searches among instance globals, returning a collection containing all
	 * globals that satisfy the given condition.
	 * 
	 * @param filter            the filtering condition to use for selecting
	 *                              which globals to return
	 * @param traverseHierarchy if {@code true}, also returns instance globals
	 *                              from superunits, transitively
	 * 
	 * @return the collection of matching globals
	 */
	private Collection<Global> searchGlobals(Predicate<Global> filter, boolean traverseHierarchy) {
		Map<String, Global> result = new HashMap<>();
		for (Global g : instanceGlobals.values())
			if (filter.test(g))
				result.put(g.getName(), g);

		if (!traverseHierarchy)
			return result.values();

		for (CompilationUnit cu : superCompilationUnits)
			for (Global sup : cu.searchGlobals(filter, true))
				if (!result.containsKey(sup.getName()))
					// we skip the ones that are hidden by globals that
					// are already in the set, since they are "hidden" from the
					// point of view of this unit
					result.put(sup.getName(), sup);

		return result.values();
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the instance cfgs defined in this unit.
	 */
	@Override
	public Collection<CFG> getAllCFGs() {
		Collection<CFG> all = super.getAllCFGs();
		instanceCfgs.values().forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the instance globals defined in this unit.
	 */
	@Override
	public Collection<Global> getAllGlobals() {
		Collection<Global> all = super.getAllGlobals();
		instanceGlobals.values().forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the instance constructs defined in this
	 * unit.
	 */
	@Override
	public Collection<NativeCFG> getAllConstructs() {
		Collection<NativeCFG> all = super.getAllConstructs();
		instanceConstructs.values().forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method returns the union of {@link #getInstanceCFGs(boolean)} and
	 * {@link #getInstanceConstructs(boolean)}.
	 */
	@Override
	public final Collection<CodeMember> getInstanceCodeMembers(boolean traverseHierarchy) {
		Set<CodeMember> all = new HashSet<>(getInstanceCFGs(traverseHierarchy));
		all.addAll(getSignatureCFGs(traverseHierarchy));
		all.addAll(getInstanceConstructs(traverseHierarchy));
		return all;
	}

	@Override
	public final boolean isInstanceOf(UnitWithSuperUnits unit) {
		return this == unit || (hierarchyComputed ? unit.instances.contains(this)
				: superCompilationUnits.stream().anyMatch(u -> u.isInstanceOf(unit)));
	}

	private final void addInstance(CompilationUnit unit) throws ProgramValidationException {
		if (superCompilationUnits.contains(unit))
			throw new ProgramValidationException("Found loop in compilation units hierarchy: " + unit
					+ " is both a super unit and an instance of " + this);
		instances.add(unit);

		for (CompilationUnit sup : superCompilationUnits)
			sup.addInstance(unit);

		for (InterfaceUnit sup : superInterfaceUnits)
			sup.addInstance(unit);
	}

	/**
	 * {@inheritDoc} <br>
	 * <br>
	 * Validating a compilation unit causes the validation of all its super
	 * units, and the population of the set of instances
	 * ({@link #getInstances()}) of each element in its hierarchy. Moreover, the
	 * validation ensures that no duplicate instance code members are defined in
	 * the compilation unit, according to
	 * {@link CFGDescriptor#matchesSignature(CFGDescriptor)}, to avoid ambiguous
	 * call resolutions. Instance code members are also linked to other ones in
	 * the hierarchy, populating the collections
	 * {@link CFGDescriptor#overriddenBy()} and
	 * {@link CFGDescriptor#overrides()}.
	 */
	@Override
	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		super.validateAndFinalize();

		for (CompilationUnit sup : superCompilationUnits)
			if (sup.sealed)
				throw new ProgramValidationException(this + " cannot inherit from the sealed unit " + sup);
			else
				sup.validateAndFinalize();

		for (InterfaceUnit i : superInterfaceUnits)
			i.validateAndFinalize();

		addInstance(this);

		for (CodeMember cfg : getInstanceCodeMembers(false)) {
			Collection<CodeMember> matching = getMatchingInstanceCodeMembers(cfg.getDescriptor(), false);
			if (matching.size() != 1 || matching.iterator().next() != cfg)
				throw new ProgramValidationException(
						cfg.getDescriptor().getSignature() + " is duplicated within unit " + this);
		}

		if (!signatureCfgs.isEmpty() && !abstractUnit)
			throw new ProgramValidationException(this + " is not an abstract class and it cannot have signature cfgs.");

		for (CompilationUnit s : superCompilationUnits)
			for (CodeMember sup : s.getInstanceCodeMembers(true)) {
				Collection<CodeMember> overriding = getMatchingInstanceCodeMembers(sup.getDescriptor(), false);
				if (overriding.size() > 1)
					throw new ProgramValidationException(
							sup.getDescriptor().getSignature() + " is overriden multiple times in unit " + this + ": "
									+ StringUtils.join(", ", overriding));
				else if (!overriding.isEmpty()) {
					if (!sup.getDescriptor().isOverridable()) {
						throw new ProgramValidationException(
								this + " overrides the non-overridable cfg " + sup.getDescriptor().getSignature());
					} else {
						CodeMember over = overriding.iterator().next();
						over.getDescriptor().overrides().addAll(sup.getDescriptor().overrides());
						over.getDescriptor().overrides().add(sup);
						over.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(over));
					}
				} else if (!s.canBeInstantiated() && canBeInstantiated())
					throw new ProgramValidationException(
							this + " does not overrides the cfg " + sup.getDescriptor().getSignature()
									+ " of the non-instantiable unit " + s);
			}

		for (InterfaceUnit i : superInterfaceUnits)
			for (ICFG sup : i.getInstanceCFGs(true)) {
				Collection<CodeMember> implementing = getMatchingInstanceCodeMembers(sup.getDescriptor(), false);
				if (implementing.size() > 1)
					throw new ProgramValidationException(
							sup.getDescriptor().getSignature() + " is implemented multiple times in unit " + this + ": "
									+ StringUtils.join(", ", implementing));
				else if (implementing.isEmpty()) {
					if (sup instanceof AbstractCFG)
						throw new ProgramValidationException(
								this + " implements the interface " + i.getName() + " but does not implements the cfg "
										+ sup.getDescriptor().getSignature());
				} else {
					CodeMember over = implementing.iterator().next();
					over.getDescriptor().overrides().addAll(sup.getDescriptor().overrides());
					over.getDescriptor().overrides().add(sup);
					over.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(over));
				}
			}

		for (CompilationUnit superUnit : superCompilationUnits)
			for (Annotation ann : superUnit.getAnnotations())
				if (!ann.isInherited())
					addAnnotation(ann);

		for (CodeMember instCfg : getInstanceCodeMembers(false))
			for (CodeMember matching : instCfg.getDescriptor().overrides())
				for (Annotation ann : matching.getDescriptor().getAnnotations()) {
					if (!ann.isInherited())
						instCfg.getDescriptor().addAnnotation(ann);

					Parameter[] args = instCfg.getDescriptor().getFormals();
					Parameter[] superArgs = matching.getDescriptor().getFormals();
					for (int i = 0; i < args.length; i++)
						for (Annotation parAnn : superArgs[i].getAnnotations()) {
							if (!parAnn.isInherited())
								args[i].addAnnotation(parAnn);
						}
				}

		hierarchyComputed = true;
	}

	/**
	 * Yields the annotations of this compilation unit.
	 * 
	 * @return the annotations of this compilation unit
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Adds an annotation to the annotations of this compilation unit.
	 * 
	 * @param ann the annotation to be added
	 */
	public void addAnnotation(Annotation ann) {
		annotations.addAnnotation(ann);
	}

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	@Override
	public boolean canBeInstantiated() {
		return !abstractUnit;
	}

	/**
	 * Adds a new instance {@link AbstractCFG}, identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), to this unit. Signature cfgs must
	 * be overridden inside subunits, according to
	 * {@link CFGDescriptor#isOverridable()}.
	 * 
	 * @param cfg the cfg to add
	 * 
	 * @return {@code true} if there was no signature cfg previously associated
	 *             with the same signature, {@code false} otherwise. If this
	 *             method returns {@code false}, the given cfg is discarded.
	 */
	public boolean addSignatureCFG(AbstractCFG cfg) {
		AbstractCFG c = signatureCfgs.putIfAbsent(cfg.getDescriptor().getSignature(), cfg);
		if (c == null)
			cfg.getDescriptor().setOverridable(true);
		else
			c.getDescriptor().setOverridable(true);
		return c == null;
	}
}
