package it.unive.lisa.interprocedural.context.recursion;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.context.ContextSensitivityToken;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;

public class Recursion<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>> {

	private final Call start;

	private final CFG head;

	private final Map<CFGCall, ContextSensitivityToken> backCalls;

	private final Collection<CodeMember> nodes;

	private final ContextSensitivityToken token;

	private final CompoundState<A, H, V, T> entryState;

	public Recursion(
			Call start,
			CFG head,
			Map<CFGCall, ContextSensitivityToken> backCalls,
			Collection<CodeMember> nodes,
			ContextSensitivityToken token,
			CompoundState<A, H, V, T> entryState) {
		this.start = start;
		this.head = head;
		this.backCalls = backCalls;
		this.nodes = nodes;
		this.token = token;
		this.entryState = entryState;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((backCalls == null) ? 0 : backCalls.hashCode());
		result = prime * result + ((entryState == null) ? 0 : entryState.hashCode());
		result = prime * result + ((head == null) ? 0 : head.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
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
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
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
		if (backCalls == null) {
			if (other.backCalls != null)
				return false;
		} else if (!backCalls.equals(other.backCalls))
			return false;
		return true;
	}

	public Call getStart() {
		return start;
	}

	public CFG getHead() {
		return head;
	}

	public Map<CFGCall, ContextSensitivityToken> getBackCalls() {
		return backCalls;
	}

	public ContextSensitivityToken getToken() {
		return token;
	}

	public CompoundState<A, H, V, T> getEntryState() {
		return entryState;
	}

	public Collection<CodeMember> getInvolvedCFGs() {
		return nodes;
	}

	public boolean canBeMergedWith(Recursion<A, H, V, T> other) {
		if (head == null) {
			if (other.head != null)
				return false;
		} else if (!head.equals(other.head))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		if (entryState == null) {
			if (other.entryState != null)
				return false;
		} else if (!entryState.equals(other.entryState))
			return false;
		return true;
	}

	public Recursion<A, H, V, T> merge(Recursion<A, H, V, T> other) {
		Collection<CodeMember> mergedNodes = new HashSet<>(nodes);
		Map<CFGCall, ContextSensitivityToken> mergedBackCalls = new HashMap<>(backCalls);
		mergedNodes.addAll(other.nodes);
		mergedBackCalls.putAll(other.backCalls);
		return new Recursion<>(start, head, mergedBackCalls, mergedNodes, token, entryState);
	}
}
