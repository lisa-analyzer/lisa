package it.unive.lisa.interprocedural.recursion;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;

public class Recursion<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>> {

	private final CFGCall start;

	private final List<RecursionNode> nodes;

	private final AnalysisState<A, H, V, T> entryState;

	public Recursion(CFGCall start, List<RecursionNode> nodes, AnalysisState<A, H, V, T> entryState) {
		this.start = start;
		this.nodes = nodes;
		this.entryState = entryState;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryState == null) ? 0 : entryState.hashCode());
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
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
		if (entryState == null) {
			if (other.entryState != null)
				return false;
		} else if (!entryState.equals(other.entryState))
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
		return true;
	}
	
	public CFGCall getStart() {
		return start;
	}

	public List<RecursionNode> getNodes() {
		return nodes;
	}

	public AnalysisState<A, H, V, T> getEntryState() {
		return entryState;
	}

	public List<CFG> getInvolvedCFGs() {
		return nodes.stream().map(n -> n.getTarget()).collect(Collectors.toList());
	}
	
	public boolean equalsUpToCalls(Recursion<A, H, V, T> other) {
		if (entryState == null) {
			if (other.entryState != null)
				return false;
		} else if (!entryState.equals(other.entryState))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (nodes.size() != other.nodes.size())
			return false;
		else
			for (int i = 0; i < nodes.size(); i++)
				if (!nodes.get(i).equalsUpToCalls(other.nodes.get(i)))
					return false;
		return true;
	}

	public Recursion<A, H, V, T> merge(Recursion<A, H, V, T> other) {
		List<RecursionNode> merged = new LinkedList<>();
		for (int i = 0; i < nodes.size(); i++) {
			RecursionNode node = nodes.get(i);
			Set<CFGCall> calls = new HashSet<>(node.getCalls());
			calls.addAll(other.nodes.get(i).getCalls());
			merged.add(new RecursionNode(calls, node.getTarget()));
		}
		return new Recursion<>(start, merged, entryState);
	}
}
