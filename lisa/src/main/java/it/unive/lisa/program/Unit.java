package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A unit of the program to analyze. A unit is a logical entity that groups a
 * set of {@link Global}s, a set of {@link CFG}s and a set of
 * {@link NativeCFG}s. The signature of each of these elements is unique within
 * the unit.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Unit extends CodeElement {

	/**
	 * The name of the unit
	 */
	private final String name;

	/**
	 * The globals defined in this unit, indexed by {@link Global#getName()}
	 */
	private final Map<String, Global> globals;

	/**
	 * The cfgs defined in this unit, indexed by
	 * {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, CFG> cfgs;

	/**
	 * The constructs ({@link NativeCFG}s) defined in this unit, indexed by
	 * {@link CFGDescriptor#getSignature()}
	 */
	private final Map<String, NativeCFG> constructs;

	/**
	 * Builds a unit, defined at the given program point.
	 * 
	 * @param sourceFile the source file where the unit is defined
	 * @param line       the line where the unit is defined within the source
	 *                       file
	 * @param col        the column where the unit is defined within the source
	 *                       file
	 * @param name       the name of the unit
	 */
	protected Unit(String sourceFile, int line, int col, String name) {
		super(sourceFile, line, col);
		this.name = name;
		this.globals = new ConcurrentHashMap<>();
		this.cfgs = new ConcurrentHashMap<>();
		this.constructs = new ConcurrentHashMap<>();
	}

	/**
	 * Yields the name of the unit.
	 * 
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Yields the collection of {@link Global}s defined in this unit. Each
	 * global is uniquely identified by its name, meaning that there are no two
	 * globals having the same name in each unit.
	 * 
	 * @return the collection of globals
	 */
	public final Collection<Global> getGlobals() {
		return globals.values();
	}

	/**
	 * Yields the collection of {@link CFG}s defined in this unit. Each cfg is
	 * uniquely identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), meaning that there are no two
	 * cfgs having the same signature in each unit.
	 * 
	 * @return the collection of cfgs
	 */
	public final Collection<CFG> getCFGs() {
		return cfgs.values();
	}

	/**
	 * Yields the collection of constructs ({@link NativeCFG}s) defined in this
	 * unit. Each construct is uniquely identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), meaning that there are no two
	 * constructs having the same signature in each unit.
	 * 
	 * @return the collection of constructs
	 */
	public final Collection<NativeCFG> getConstructs() {
		return constructs.values();
	}

	/**
	 * Yields the collection of {@link CodeMember}s defined in this unit. This
	 * method returns the union of {@link #getCFGs()} and
	 * {@link #getConstructs()}.
	 * 
	 * @return the collection of code members
	 */
	public final Collection<CodeMember> getCodeMembers() {
		HashSet<CodeMember> all = new HashSet<>(getCFGs());
		all.addAll(getConstructs());
		return all;
	}

	/**
	 * Yields the {@link Global} defined in this unit having the given name
	 * ({@link Global#getName()}), if any.
	 * 
	 * @param name the name of the global to find
	 * 
	 * @return the global with the given name, or {@code null}
	 */
	public final Global getGlobal(String name) {
		return globals.get(name);
	}

	/**
	 * Yields the {@link CFG} defined in this unit having the given signature
	 * ({@link CFGDescriptor#getSignature()}), if any.
	 * 
	 * @param signature the signature of the cfg to find
	 * 
	 * @return the cfg with the given signature, or {@code null}
	 */
	public final CFG getCFG(String signature) {
		return cfgs.get(signature);
	}

	/**
	 * Yields the {@link NativeCFG} defined in this unit having the given
	 * signature ({@link CFGDescriptor#getSignature()}), if any.
	 * 
	 * @param signature the signature of the construct to find
	 * 
	 * @return the construct with the given signature, or {@code null}
	 */
	public final NativeCFG getConstruct(String signature) {
		return constructs.get(signature);
	}

	/**
	 * Yields the {@link CodeMember} defined in this unit having the given
	 * signature ({@link CFGDescriptor#getSignature()}), if any. This method
	 * first searches the code member within the cfgs defined in this unit,
	 * through {@link #getCFG(String)}, and then within the constructs through
	 * {@link #getConstruct(String)}, returning the first non-null result.
	 * 
	 * @param signature the signature of the code member to find
	 * 
	 * @return the code member with the given signature, or {@code null}
	 */
	public final CodeMember getCodeMember(String signature) {
		CodeMember res;
		if ((res = getCFG(signature)) != null)
			return res;

		return getConstruct(signature);
	}

	/**
	 * Yields the collection of all {@link CFG}s defined in this unit that have
	 * the given name.
	 * 
	 * @param name the name of the cfgs to include
	 * 
	 * @return the collection of cfgs with the given name
	 */
	public final Collection<CFG> getCFGsByName(String name) {
		return cfgs.values().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.collect(Collectors.toList());
	}

	/**
	 * Yields the collection of all {@link NativeCFG}s defined in this unit that
	 * have the given name.
	 * 
	 * @param name the name of the constructs to include
	 * 
	 * @return the collection of constructs with the given name
	 */
	public final Collection<NativeCFG> getConstructsByName(String name) {
		return constructs.values().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.collect(Collectors.toList());
	}

	/**
	 * Yields the collection of all {@link CodeMember}s defined in this unit
	 * that have the given name. This method returns the union of
	 * {@link #getCFGsByName(String)} and {@link #getConstructsByName(String)}.
	 * 
	 * @param name the name of the code members to include
	 * 
	 * @return the collection of code members with the given name
	 */
	public final Collection<CodeMember> getCodeMembersByName(String name) {
		HashSet<CodeMember> all = new HashSet<>(getCFGsByName(name));
		all.addAll(getConstructsByName(name));
		return all;
	}

	/**
	 * Yields the collection of <b>all</b> the {@link Global}s defined in this
	 * unit.
	 * 
	 * @return the collection of the globals
	 */
	public Collection<Global> getAllGlobals() {
		return new HashSet<>(getGlobals());
	}

	/**
	 * Yields the collection of <b>all</b> the {@link CFG}s defined in this
	 * unit.
	 * 
	 * @return the collection of the cfgs
	 */
	public Collection<CFG> getAllCFGs() {
		return new HashSet<>(getCFGs());
	}

	/**
	 * Yields the collection of <b>all</b> the {@link NativeCFG}s defined in
	 * this unit.
	 * 
	 * @return the collection of the cfgs
	 */
	public Collection<NativeCFG> getAllConstructs() {
		return new HashSet<>(getConstructs());
	}

	/**
	 * Yields the collection of <b>all</b> the {@link CodeMember}s defined in
	 * this unit. This method returns the union between {@link #getAllCFGs()}
	 * and {@link #getAllConstructs()}.
	 * 
	 * @return the collection of the code members
	 */
	public final Collection<CodeMember> getAllCodeMembers() {
		HashSet<CodeMember> all = new HashSet<>(getAllCFGs());
		all.addAll(getAllConstructs());
		return all;
	}

	/**
	 * Adds a new {@link Global}, identified by its name
	 * ({@link Global#getName()}), to this unit.
	 * 
	 * @param global the global to add
	 * 
	 * @return {@code true} if there was no global previously associated with
	 *             the same name, {@code false} otherwise. If this method
	 *             returns {@code false}, the given global is discarded.
	 */
	public final boolean addGlobal(Global global) {
		return globals.putIfAbsent(global.getName(), global) == null;
	}

	/**
	 * Adds a new {@link CFG}, identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), to this unit.
	 * 
	 * @param cfg the cfg to add
	 * 
	 * @return {@code true} if there was no cfg previously associated with the
	 *             same signature, {@code false} otherwise. If this method
	 *             returns {@code false}, the given cfg is discarded.
	 */
	public final boolean addCFG(CFG cfg) {
		return cfgs.putIfAbsent(cfg.getDescriptor().getSignature(), cfg) == null;
	}

	/**
	 * Adds a new {@link NativeCFG}, identified by its signature
	 * ({@link CFGDescriptor#getSignature()}), to this unit.
	 * 
	 * @param construct the construct to add
	 * 
	 * @return {@code true} if there was no construct previously associated with
	 *             the same signature, {@code false} otherwise. If this method
	 *             returns {@code false}, the given construct is discarded.
	 */
	public final boolean addConstruct(NativeCFG construct) {
		return constructs.putIfAbsent(construct.getDescriptor().getSignature(), construct) == null;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Unit other = (Unit) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return name;
	}

	/**
	 * Finds all the code members whose signature matches the one of the given
	 * {@link CFGDescriptor}, according to
	 * {@link CFGDescriptor#matchesSignature(CFGDescriptor)}.
	 * 
	 * @param signature the descriptor providing the signature to match
	 * 
	 * @return the collection of code members that match the given signature
	 */
	public final Collection<CodeMember> getMatchingCodeMember(CFGDescriptor signature) {
		Collection<CodeMember> result = new HashSet<>();

		for (CFG cfg : cfgs.values())
			if (cfg.getDescriptor().matchesSignature(signature))
				result.add(cfg);

		for (NativeCFG construct : constructs.values())
			if (construct.getDescriptor().matchesSignature(signature))
				result.add(construct);

		return result;
	}

	/**
	 * Validates this unit, ensuring its consistency. This ensures that no two
	 * code members exist in this unit whose signatures matches one another,
	 * according to {@link CFGDescriptor#matchesSignature(CFGDescriptor)}. This
	 * avoids ambiguous call resolution. Moreover, this ensures that all
	 * {@link CFG}s are valid, according to {@link CFG#validate()}.
	 * 
	 * @throws ProgramValidationException if the program has an invalid
	 *                                        structure
	 */
	public void validateAndFinalize() throws ProgramValidationException {
		for (CodeMember cfg : getCodeMembers()) {
			Collection<CodeMember> matching = getMatchingCodeMember(cfg.getDescriptor());
			if (matching.size() != 1 || matching.iterator().next() != cfg)
				throw new ProgramValidationException(
						cfg.getDescriptor().getSignature() + " is duplicated within unit " + this);
		}

		for (CFG cfg : getAllCFGs())
			cfg.validate();
	}
}
