package it.unive.lisa.type;

import it.unive.lisa.program.cfg.CodeMember;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * The type of function references, used to point to one or more
 * {@link CodeMember}s in the program.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CodeMemberType
		implements
		Type {

	private final Set<CodeMember> members;

	/**
	 * Builds the code member type pointing to the given members.
	 *
	 * @param members the members
	 */
	public CodeMemberType(
			Set<CodeMember> members) {
		this.members = members;
	}

	/**
	 * Yields the {@link CodeMember}s referenced by this type.
	 *
	 * @return the members
	 */
	public Set<CodeMember> getMembers() {
		return members;
	}

	@Override
	public String toString() {
		Set<String> sorted = new TreeSet<>();
		for (CodeMember m : members)
			sorted.add(m.getDescriptor().toString());
		return "coderef::" + sorted;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CodeMemberType other = (CodeMemberType) obj;
		if (members == null) {
			if (other.members != null)
				return false;
		} else if (!members.equals(other.members))
			return false;
		return true;
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return other instanceof CodeMemberType || other.isUntyped();
	}

	@Override
	public Type commonSupertype(
			Type other) {
		// unlike most other Type implementations in this package,
		// CodeMemberType is not a singleton: distinct instances wrapping the
		// same set of members are common and must be recognized as the same
		// type here, consistently with equals()
		return equals(other) ? this : Untyped.INSTANCE;
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

}
