package it.unive.lisa.program.cfg.fixpoints;

import java.util.HashMap;
import java.util.Map;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;

public class AscendingFixpoint<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends CFGFixpoint<A, H, V, T> {

	private final int widenAfter;
	private final Map<Statement, Integer> lubs;

	public AscendingFixpoint(CFG target, int widenAfter,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(target, interprocedural);
		this.widenAfter = widenAfter;
		this.lubs = new HashMap<>(target.getNodesCount());
	}

	@Override
	public CompoundState<A, H, V, T> operation(Statement node,
			CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		CompoundState<A, H, V, T> result;
		
		if (widenAfter == 0)
			result = approx.lub(old);
		else {
			// we multiply by the number of predecessors since
			// if we have more than one
			// the threshold will be reached faster
			int lub = lubs.computeIfAbsent(node, st -> widenAfter * graph.predecessorsOf(st).size());
			if (lub > 0) 
				result = approx.lub(old);
			else 
				result = approx.widening(old);
			lubs.put(node, --lub);
		}

		return result;
	}

	@Override
	public boolean equality(Statement node, CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		return approx.lessOrEqual(old);
	}
}
