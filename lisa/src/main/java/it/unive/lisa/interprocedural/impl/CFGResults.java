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

	public boolean putResult(ContextSensitiveToken token, CFGWithAnalysisResults<A, H, V> result)
			throws InterproceduralAnalysisException, SemanticException {
		CFGWithAnalysisResults<A, H, V> previousResult = function.get(token);
		if (previousResult == null) {
			// no previous result
			function.put(token, result);
			return false;
		} else if (previousResult.lessOrEqual(result)) {
			// previous is smaller than result
			if (result.lessOrEqual(previousResult))
				// they are equal
				return false;
			else {
				// result is bigger, store that instead
				function.put(token, result);
				return true;
			}
		} else if (result.lessOrEqual(previousResult)) {
			// result is smaller than previous
			return false;
		} else {
			// result and previous are not comparable
			function.put(token, previousResult.lub(result));
			return true;
		}
	}

	public boolean contains(ContextSensitiveToken token) {
		return function != null && function.containsKey(token);
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
