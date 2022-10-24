package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A unit of the program to analyze. A unit is a logical entity that groups a
 * set of {@link Global}s, a set of {@link CFG}s and a set of
 * {@link NativeCFG}s. The signature of each of these elements is unique within
 * the unit.<br>
 * <br>
 * Note that this class does not implement {@link #equals(Object)} nor
 * {@link #hashCode()} since all units are unique.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Unit {

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
	 * {@link CodeMemberDescriptor#getSignature()}
	 */
	private final Map<String, CodeMember> codeMembers;

	/**
	 * Builds a unit, defined at the given location.
	 * 
	 * @param name the name of the unit
	 */
	protected Unit(String name) {
		this.name = name;
		this.globals = new TreeMap<>();
		this.codeMembers = new TreeMap<>();
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
	 * Yields the collection of {@link CodeMember}s defined in this unit. Each
	 * code member is uniquely identified by its signature
	 * ({@link CodeMemberDescriptor#getSignature()}), meaning that there are no
	 * two cfgs having the same signature in each unit.
	 * 
	 * @return the collection of code members
	 */
	public final Collection<CodeMember> getCodeMembers() {
		return codeMembers.values();
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
	 * Yields the {@link CodeMember} defined in this unit having the given
	 * signature ({@link CodeMemberDescriptor#getSignature()}), if any.
	 * 
	 * @param signature the signature of the code member to find
	 * 
	 * @return the code member with the given signature, or {@code null}
	 */
	public final CodeMember getCodeMember(String signature) {
		return codeMembers.get(signature);
	}

	/**
	 * Yields the collection of all {@link CodeMember}s defined in this unit
	 * that have the given name.
	 * 
	 * @param name the name of the code members to include
	 * 
	 * @return the collection of code members with the given name
	 */
	public final Collection<CodeMember> getCodeMembersByName(String name) {
		return codeMembers.values().stream().filter(c -> c.getDescriptor().getName().equals(name))
				.collect(Collectors.toList());
	}

	/**
	 * Yields the collection of <b>all</b> the {@link Global}s defined in this
	 * unit.
	 * 
	 * @return the collection of the globals
	 */
	public Collection<Global> getGlobalsRecursively() {
		return new HashSet<>(getGlobals());
	}

	/**
	 * Yields the collection of <b>all</b> the {@link CodeMember}s defined in
	 * this unit. This method returns the same result as
	 * {@link #getCodeMembers()}, but subclasses are likely to re-implement it
	 * to add additional ones (e.g., instance members).
	 * 
	 * @return the collection of the code members
	 */
	public Collection<CodeMember> getCodeMembersRecursively() {
		return new HashSet<>(getCodeMembers());
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
	 * Adds a new {@link CodeMember}, identified by its signature
	 * ({@link CodeMemberDescriptor#getSignature()}), to this unit.
	 * 
	 * @param member the member to add
	 * 
	 * @return {@code true} if there was no member previously associated with
	 *             the same signature, {@code false} otherwise. If this method
	 *             returns {@code false}, the given member is discarded.
	 */
	public final boolean addCodeMember(CodeMember member) {
		return codeMembers.putIfAbsent(member.getDescriptor().getSignature(), member) == null;
	}

	@Override
	public final String toString() {
		return name;
	}

	/**
	 * Finds all the code members whose signature matches the one of the given
	 * {@link CodeMemberDescriptor}, according to
	 * {@link CodeMemberDescriptor#matchesSignature(CodeMemberDescriptor)}.
	 * 
	 * @param signature the descriptor providing the signature to match
	 * 
	 * @return the collection of code members that match the given signature
	 */
	public final Collection<CodeMember> getMatchingCodeMember(CodeMemberDescriptor signature) {
		Collection<CodeMember> result = new HashSet<>();

		for (CodeMember member : codeMembers.values())
			if (member.getDescriptor().matchesSignature(signature))
				result.add(member);

		return result;
	}

	/**
	 * Yields {@code true} if this unit can be instantiated, {@code false}
	 * otherwise (e.g., interfaces, abstract classes).
	 * 
	 * @return {@code true} if this unit can be instantiated, {@code false}
	 *             otherwise
	 */
	public abstract boolean canBeInstantiated();

	/**
	 * Yields the {@link Program} where this unit is defined.
	 * 
	 * @return the program
	 */
	public abstract Program getProgram();
}
