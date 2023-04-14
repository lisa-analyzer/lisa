package it.unive.lisa.interprocedural.context.recursion;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.context.ContextSensitivityToken;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.call.Call;
import java.util.Collection;

public class Recursion<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>> {

	private final Call start;

	private final CFG head;

	private final Collection<CodeMember> nodes;

	private final ContextSensitivityToken invocationToken;

	private final CompoundState<A, H, V, T> entryState;

	public Recursion(
			Call start,
			CFG head,
			Collection<CodeMember> nodes,
			ContextSensitivityToken invocationToken,
			CompoundState<A, H, V, T> entryState) {
		this.start = start;
		this.head = head;
		this.nodes = nodes;
		this.invocationToken = invocationToken;
		this.entryState = entryState;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryState == null) ? 0 : entryState.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((invocationToken == null) ? 0 : invocationToken.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Recursion<?, ?, ?, ?> other = (Recursion<?, ?, ?, ?>) obj;
		if (invocationToken == null) {
			if (other.invocationToken != null)
				return false;
		} else if (!invocationToken.equals(other.invocationToken))
			return false;
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (entryState == null) {
			if (other.entryState != null)
				return false;
		} else if (!entryState.equals(other.entryState))
			return false;
		return true;
	}

	public Call getStart() {
		return start;
	}

	public CFG getHead() {
		return head;
	}

	public ContextSensitivityToken getInvocationToken() {
		return invocationToken;
	}

	public CompoundState<A, H, V, T> getEntryState() {
		return entryState;
	}

	public Collection<CodeMember> getInvolvedCFGs() {
		return nodes;
	}
}
