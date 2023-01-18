package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;

public class DescendingNarrowingFixpoint<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends CFGFixpoint<A, H, V, T> {

	public DescendingNarrowingFixpoint(CFG target,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(target, interprocedural);
	}

	@Override
	public CompoundState<A, H, V, T> operation(Statement node,
			CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		AnalysisState<A, H, V, T> newApprox = approx.postState, oldApprox = old.postState;
		StatementStore<A, H, V, T> newIntermediate = approx.intermediateStates,
				oldIntermediate = old.intermediateStates;

		newApprox = oldApprox.narrowing(newApprox);
		newIntermediate = oldIntermediate.narrowing(newIntermediate);

		return CompoundState.of(newApprox, newIntermediate);
	}

	@Override
	public boolean equality(Statement node, CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		return old.postState.lessOrEqual(approx.postState)
				&& old.intermediateStates.lessOrEqual(approx.intermediateStates);
	}
}
