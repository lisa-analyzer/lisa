package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.program.cfg.CFG;

public class FixpointResults<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> extends FunctionalLattice<FixpointResults<A, H, V>, CFG, CFGResults<A, H, V>> {

	private boolean lubbedSomething;
	
	public FixpointResults(CFGResults<A, H, V> lattice) {
		super(lattice);
		this.lubbedSomething = false;
	}

	public void putResult(CFG cfg, ContextSensitiveToken token, CFGWithAnalysisResults<A, H, V> result)
			throws InterproceduralAnalysisException, SemanticException {
		CFGResults<A, H, V> res = function.computeIfAbsent(cfg, c -> new CFGResults<>(result.top()));
		lubbedSomething |= res.putResult(token, result);
	}
	
	public void newIteration() {
		lubbedSomething = false;
	}
	
	public boolean lubbedSomething() {
		return lubbedSomething;
	}

	public boolean contains(CFG cfg) {
		return function != null && function.containsKey(cfg);
	}
	
	@Override
	public FixpointResults<A, H, V> top() {
		return new FixpointResults<>(lattice.top());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && (function == null || function.isEmpty());
	}

	@Override
	public FixpointResults<A, H, V> bottom() {
		return new FixpointResults<>(lattice.bottom());
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && (function == null || function.isEmpty());
	}
}
