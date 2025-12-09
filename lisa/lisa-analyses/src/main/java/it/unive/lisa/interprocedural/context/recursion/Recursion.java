package it.unive.lisa.interprocedural.context.recursion;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.interprocedural.context.KDepthToken;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.call.Call;
import java.util.Collection;

/**
 * A recursion happening in the program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} contained into the analysis
 *                state
 */
public class Recursion<A extends AbstractLattice<A>> {

	private final Call start;

	private final CFG head;

	private final Collection<CodeMember> members;

	private final KDepthToken<A> invocationToken;

	private final CompoundState<A> entryState;

	/**
	 * Builds the recursion.
	 * 
	 * @param invocation      the call that started the recursion
	 * @param invocationToken the active token when {@code invocation} was
	 *                            executed
	 * @param entryState      the entry state (prestate of {@code invocation}
	 *                            and poststates of its parameters) of the
	 *                            recursion
	 * @param recursionHead   the member of the recursion that was invoked by
	 *                            {@code invocation}
	 * @param members         the members that are part of the recursion
	 */
	public Recursion(
			Call invocation,
			KDepthToken<A> invocationToken,
			CompoundState<A> entryState,
			CFG recursionHead,
			Collection<CodeMember> members) {
		this.start = invocation;
		this.head = recursionHead;
		this.members = members;
		this.invocationToken = invocationToken;
		this.entryState = entryState;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryState == null) ? 0 : entryState.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		result = prime * result + ((invocationToken == null) ? 0 : invocationToken.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
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
		Recursion<?> other = (Recursion<?>) obj;
		if (entryState == null) {
			if (other.entryState != null)
				return false;
		} else if (!entryState.equals(other.entryState))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		if (invocationToken == null) {
			if (other.invocationToken != null)
				return false;
		} else if (!invocationToken.equals(other.invocationToken))
			return false;
		if (members == null) {
			if (other.members != null)
				return false;
		} else if (!members.equals(other.members))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	/**
	 * Yields the call that started this recursion by calling
	 * {@link #getRecursionHead()}.
	 * 
	 * @return the call that invoked the recursion
	 */
	public Call getInvocation() {
		return start;
	}

	/**
	 * Yields the head of the recursion, that is, the first member that was
	 * invoked from outside the recursion by {@link #getInvocation()}.
	 * 
	 * @return the head of the recursion
	 */
	public CFG getRecursionHead() {
		return head;
	}

	/**
	 * Yields the {@link KDepthToken} that was active when
	 * {@link #getInvocation()} was executed to start the recursion.
	 * 
	 * @return the token
	 */
	public KDepthToken<A> getInvocationToken() {
		return invocationToken;
	}

	/**
	 * Yields the entry state (prestate of {@link #getInvocation()} together
	 * with the poststates of its parameters) that was used to start this
	 * recursion.
	 * 
	 * @return the entry state
	 */
	public CompoundState<A> getEntryState() {
		return entryState;
	}

	/**
	 * Yields all the {@link CodeMember}s part of this recursion.
	 * 
	 * @return the members
	 */
	public Collection<CodeMember> getMembers() {
		return members;
	}

	@Override
	public String toString() {
		return members.toString() + " (started at " + start.getLocation() + ")";
	}

}
