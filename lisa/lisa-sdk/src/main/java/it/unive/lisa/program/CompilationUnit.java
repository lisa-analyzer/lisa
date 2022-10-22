package it.unive.lisa.program;

import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.AbstractCodeMember;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

/**
 * An unit of the program to analyze that is part of a hierarchical structure.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">VincenzoArceri</a>
 */
public abstract class CompilationUnit extends ProgramUnit {

	/**
	 * The instance globals defined in this unit, indexed by
	 * {@link Global#getName()}
	 */
	private final Map<String, Global> instanceGlobals;

	/**
	 * The instance code members defined in this unit, indexed by
	 * {@link CodeMemberDescriptor#getSignature()}
	 */
	private final Map<String, CodeMember> instanceCodeMembers;

	/**
	 * The lazily computed collection of instances of this unit, that is, the
	 * collection of compilation units that directly or indirectly inherit from
	 * this unit.
	 */
	protected final Collection<Unit> instances;

	/**
	 * The annotations of this unit
	 */
	private final Annotations annotations;

	/**
	 * Whether or not this compilation unit is sealed, meaning that it cannot be
	 * used as super unit of other compilation units
	 */
	private final boolean sealed;

	/**
	 * Whether or not the hierarchy of this interface unit has been fully
	 * computed, to avoid re-computation.
	 */
	protected boolean hierarchyComputed;

	/**
	 * Builds an unit with super unit.
	 * 
	 * @param location the location where the unit is define within the source
	 *                     file
	 * @param program  the program where this unit is defined
	 * @param name     the name of the unit
	 * @param sealed   whether or not this unit can be inherited from
	 */
	protected CompilationUnit(CodeLocation location, Program program, String name, boolean sealed) {
		super(location, program, name);
		this.sealed = sealed;
		instanceCodeMembers = new TreeMap<>();
		instanceGlobals = new TreeMap<>();
		instances = new HashSet<>();
		annotations = new Annotations();

		hierarchyComputed = false;
	}

	/**
	 * Yields whether or not this unit is sealed, meaning that it cannot be used
	 * as super unit of other compilation units.
	 * 
	 * @return {@code true} if this unit is sealed
	 */
	public boolean isSealed() {
		return sealed;
	}

	/**
	 * Adds a new {@link CompilationUnit} as direct inheritance ancestor (i.e.,
	 * superclass, interface, or superinterface) of this unit.
	 * 
	 * @param unit the unit to add
	 * 
	 * @return {@code true} if the collection of ancestors changed as a result
	 *             of the call
	 */
	public abstract boolean addAncestor(CompilationUnit unit);

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the instance code members defined in this
	 * unit.
	 */
	@Override
	public Collection<CodeMember> getCodeMembersRecursively() {
		Collection<CodeMember> all = super.getCodeMembersRecursively();
		instanceCodeMembers.values().forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the instance globals defined in this unit.
	 */
	@Override
	public Collection<Global> getGlobalsRecursively() {
		Collection<Global> all = super.getGlobalsRecursively();
		instanceGlobals.values().forEach(all::add);
		return all;
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
	public Collection<CodeMember> getInstanceCodeMembers(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> true, traverseHierarchy);
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
	public Collection<Global> getInstanceGlobals(boolean traverseHierarchy) {
		return searchGlobals(g -> true, traverseHierarchy);
	}

	/**
	 * Yields the collection of instance {@link CFG}s defined in this unit. Each
	 * cfg is uniquely identified by its signature
	 * ({@link CodeMemberDescriptor#getSignature()}), meaning that there are no
	 * two instance cfgs having the same signature in each unit. Instance cfgs
	 * can be overridden inside subunits, according to
	 * {@link CodeMemberDescriptor#isOverridable()}.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns instance cfgs from
	 *                              superunits, transitively
	 * 
	 * @return the collection of instance cfgs
	 */
	public Collection<CFG> getInstanceCFGs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof CFG, traverseHierarchy);
	}

	/**
	 * Yields the collection of instance {@link AbstractCodeMember}s defined in
	 * this unit. Each cfg is uniquely identified by its signature
	 * ({@link CodeMemberDescriptor#getSignature()}), meaning that there are no
	 * two signature cfgs having the same signature in each unit. Signature cfgs
	 * must be overridden inside subunits, according to
	 * {@link CodeMemberDescriptor#isOverridable()}.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns signature cfgs
	 *                              from superunits, transitively
	 * 
	 * @return the collection of signature cfgs
	 */
	public Collection<AbstractCodeMember> getAbstractCodeMembers(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof AbstractCodeMember, traverseHierarchy);
	}

	/**
	 * Yields the collection of instance constructs ({@link NativeCFG}s) defined
	 * in this unit. Each construct is uniquely identified by its signature
	 * ({@link CodeMemberDescriptor#getSignature()}), meaning that there are no
	 * two instance constructs having the same signature in each unit. Instance
	 * constructs can be overridden inside subunits, according to
	 * {@link CodeMemberDescriptor#isOverridable()}.
	 * 
	 * @param traverseHierarchy if {@code true}, also returns instance
	 *                              constructs from superunits, transitively
	 * 
	 * @return the collection of instance constructs
	 */
	public Collection<NativeCFG> getInstanceConstructs(boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm instanceof NativeCFG, traverseHierarchy);
	}

	/**
	 * Yields the collection of {@link ClassUnit}s that are instances of this
	 * one, including itself. In other words, this method returns the collection
	 * of compilation units that directly or indirectly, inherit from this
	 * one.<br>
	 * <br>
	 * Note that this method returns an empty collection, until
	 * {@link #validateAndFinalize()} has been called.
	 * 
	 * @return the collection of units that are instances of this one, including
	 *             this unit itself
	 */
	public Collection<Unit> getInstances() {
		return instances;
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

	/**
	 * Yields the collection of {@link CompilationUnit}s that are this unit
	 * directly inherits from, regardless of their type.
	 * 
	 * @return the collection of units that are direct ancestors of this one
	 */
	public abstract Collection<CompilationUnit> getImmediateAncestors();

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
	public abstract boolean isInstanceOf(CompilationUnit unit);

	/**
	 * Searches among instance code members, returning a collection containing
	 * all members that satisfy the given condition.
	 * 
	 * @param <T>               the concrete type of elements that this method
	 *                              returns
	 * @param filter            the filtering condition to use for selecting
	 *                              which code members to return
	 * @param traverseHierarchy if {@code true}, also returns instance code
	 *                              members from superunits, transitively
	 * 
	 * @return the collection of matching code members
	 */
	@SuppressWarnings("unchecked")
	protected <T extends CodeMember> Collection<T> searchCodeMembers(Predicate<CodeMember> filter,
			boolean traverseHierarchy) {
		Collection<T> result = new HashSet<>();

		for (CodeMember member : instanceCodeMembers.values())
			if (filter.test(member))
				result.add((T) member);

		if (!traverseHierarchy)
			return result;

		for (CompilationUnit cu : getImmediateAncestors())
			for (CodeMember sup : cu.searchCodeMembers(filter, true))
				if (!result.stream().anyMatch(cm -> sup.getDescriptor().overriddenBy().contains(cm)))
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
	protected Collection<Global> searchGlobals(Predicate<Global> filter, boolean traverseHierarchy) {
		Map<String, Global> result = new HashMap<>();
		for (Global g : instanceGlobals.values())
			if (filter.test(g))
				result.put(g.getName(), g);

		if (!traverseHierarchy)
			return result.values();

		for (CompilationUnit cu : getImmediateAncestors())
			for (Global sup : cu.searchGlobals(filter, true))
				if (!result.containsKey(sup.getName()))
					// we skip the ones that are hidden by globals that
					// are already in the set, since they are "hidden" from the
					// point of view of this unit
					result.put(sup.getName(), sup);

		return result.values();
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
	public boolean addInstanceGlobal(Global global) {
		return instanceGlobals.putIfAbsent(global.getName(), global) == null;
	}

	/**
	 * Adds a new instance {@link CodeMember}, identified by its signature
	 * ({@link CodeMemberDescriptor#getSignature()}), to this unit. Instance
	 * code members can be overridden inside subunits, according to
	 * {@link CodeMemberDescriptor#isOverridable()}.
	 * 
	 * @param cm the cfg to add
	 * 
	 * @return {@code true} if there was no instance member previously
	 *             associated with the same signature, {@code false} otherwise.
	 *             If this method returns {@code false}, the given code member
	 *             is discarded.
	 */
	public boolean addInstanceCodeMember(CodeMember cm) {
		CodeMember c = instanceCodeMembers.putIfAbsent(cm.getDescriptor().getSignature(), cm);
		if (sealed)
			if (c == null)
				cm.getDescriptor().setOverridable(false);
			else
				c.getDescriptor().setOverridable(false);
		return c == null;
	}

	/**
	 * Yields the first instance {@link CodeMember} defined in this unit having
	 * the given signature ({@link CodeMemberDescriptor#getSignature()}), if
	 * any.
	 * 
	 * @param signature         the signature of the member to find
	 * @param traverseHierarchy if {@code true}, also returns instance members
	 *                              from superunits, transitively
	 * 
	 * @return the instance code member with the given signature, or
	 *             {@code null}
	 */
	public CodeMember getInstanceCodeMember(String signature, boolean traverseHierarchy) {
		Collection<CodeMember> res = searchCodeMembers(cm -> cm.getDescriptor().getSignature().equals(signature),
				traverseHierarchy);
		if (res.isEmpty())
			return null;
		return res.stream().findFirst().get();
	}

	/**
	 * Yields the first instance {@link Global} defined in this unit having the
	 * given name ({@link Global#getName()}), if any.
	 * 
	 * @param name              the name of the global to find
	 * @param traverseHierarchy if {@code true}, also returns instance globals
	 *                              from superunits, transitively
	 * 
	 * @return the instance global with the given name, or {@code null}
	 */
	public Global getInstanceGlobal(String name, boolean traverseHierarchy) {
		Collection<Global> res = searchGlobals(cm -> cm.getName().equals(name), traverseHierarchy);
		if (res.isEmpty())
			return null;
		return res.stream().findFirst().get();
	}

	/**
	 * Yields the collection of all instance {@link CodeMember}s defined in this
	 * unit that have the given name.
	 * 
	 * @param name              the name of the members to include
	 * @param traverseHierarchy if {@code true}, also returns instance members
	 *                              from superunits, transitively
	 * 
	 * @return the collection of instance members with the given name
	 */
	public Collection<CodeMember> getInstanceCodeMembersByName(String name, boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm.getDescriptor().getName().equals(name), traverseHierarchy);
	}

	/**
	 * Finds all the instance code members whose signature matches the one of
	 * the given {@link CodeMemberDescriptor}, according to
	 * {@link CodeMemberDescriptor#matchesSignature(CodeMemberDescriptor)}.
	 * 
	 * @param signature         the descriptor providing the signature to match
	 * @param traverseHierarchy if {@code true}, also returns instance code
	 *                              members from superunits, transitively
	 * 
	 * @return the collection of instance code members that match the given
	 *             signature
	 */
	public Collection<CodeMember> getMatchingInstanceCodeMembers(CodeMemberDescriptor signature,
			boolean traverseHierarchy) {
		return searchCodeMembers(cm -> cm.getDescriptor().matchesSignature(signature), traverseHierarchy);
	}

	/**
	 * {@inheritDoc} <br>
	 * <br>
	 * Validating a compilation unit causes the validation of all its super
	 * units, and the population of the set of instances
	 * ({@link #getInstances()}) of each element in its hierarchy. Moreover, the
	 * validation ensures that no duplicate instance code members are defined in
	 * the compilation unit, according to
	 * {@link CodeMemberDescriptor#matchesSignature(CodeMemberDescriptor)}, to
	 * avoid ambiguous call resolutions. Instance code members are also linked
	 * to other ones in the hierarchy, populating the collections
	 * {@link CodeMemberDescriptor#overriddenBy()} and
	 * {@link CodeMemberDescriptor#overrides()}.
	 */
	@Override
	public void validateAndFinalize() throws ProgramValidationException {
		if (hierarchyComputed)
			return;

		super.validateAndFinalize();

		// recursive invocation
		for (CompilationUnit sup : getImmediateAncestors())
			if (sup.isSealed())
				throw new ProgramValidationException(this + " cannot inherit from the sealed unit " + sup);
			else
				sup.validateAndFinalize();

		// check for duplicate cms
		for (CodeMember cm : getInstanceCodeMembers(false)) {
			Collection<CodeMember> matching = getMatchingInstanceCodeMembers(cm.getDescriptor(), false);
			if (matching.size() != 1 || matching.iterator().next() != cm)
				throw new ProgramValidationException(
						cm.getDescriptor().getSignature() + " is duplicated within unit " + this);
		}

		for (CompilationUnit ancestor : getImmediateAncestors()) {
			// check overriders/implementers
			for (CodeMember inherited : ancestor.getInstanceCodeMembers(true)) {
				Collection<CodeMember> localOverrides = getMatchingInstanceCodeMembers(inherited.getDescriptor(),
						false);
				if (localOverrides.isEmpty()) {
					if (inherited instanceof AbstractCodeMember && !ancestor.canBeInstantiated() && canBeInstantiated())
						// this is the first non-abstract child of ancestor, and
						// it must provide an implementation for all abstract
						// code members defined in the inheritance chain
						throw new ProgramValidationException(this + " does not overrides the cfg "
								+ inherited.getDescriptor().getSignature() + " of the non-instantiable unit "
								+ ancestor);
				} else if (localOverrides.size() == 1) {
					if (!inherited.getDescriptor().isOverridable()) {
						throw new ProgramValidationException(
								this + " overrides the non-overridable cfg "
										+ inherited.getDescriptor().getSignature());
					} else {
						CodeMember over = localOverrides.iterator().next();
						over.getDescriptor().overrides().addAll(inherited.getDescriptor().overrides());
						over.getDescriptor().overrides().add(inherited);
						over.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(over));
					}
				} else {
					throw new ProgramValidationException(inherited.getDescriptor().getSignature()
							+ " is overriden multiple times in unit " + this + ": "
							+ StringUtils.join(", ", localOverrides));
				}
			}

			// propagate annotations
			for (Annotation ann : ancestor.getAnnotations())
				if (!ann.isInherited())
					addAnnotation(ann);
		}

		// propagate annotations in cfgs - this has to be done after the
		// override chain has been computed
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
}
