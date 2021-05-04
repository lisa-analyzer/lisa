package it.unive.lisa.interprocedural.impl;

import java.util.Collection;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;

public class CFGResults<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>>
		extends FunctionalLattice<CFGResults<A, H, V>, ContextSensitiveToken, CFGWithAnalysisResults<A, H, V>> {

	public CFGResults(CFGWithAnalysisResults<A, H, V> lattice) {
		super(lattice);
	}

	public void putResult(ContextSensitiveToken token, CFGWithAnalysisResults<A, H, V> result)
			throws InterproceduralAnalysisException, SemanticException {
		CFGWithAnalysisResults<A, H, V> previousResult = function.get(token);
		if (previousResult == null)
			function.put(token, result);
		else {
			if (!previousResult.getEntryState().lessOrEqual(result.getEntryState()))
				throw new InterproceduralAnalysisException(
						"Cannot reduce the entry state in the interprocedural analysis");
		}
	}

	public Collection<CFGWithAnalysisResults<A, H, V>> getAll() {
		return function.values();
	}

	@Override
	public CFGResults<A, H, V> top() {
		return new CFGResults<>(lattice.top());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && (function == null || function.isEmpty());
	}

	@Override
	public CFGResults<A, H, V> bottom() {
		return new CFGResults<>(lattice.bottom());
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && (function == null || function.isEmpty());
	}
}
