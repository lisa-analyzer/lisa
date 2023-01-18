package it.unive.lisa.program.cfg.fixpoints;

import java.util.HashMap;
import java.util.Map;

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

public class DescendingGLBFixpoint<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends CFGFixpoint<A, H, V, T> {

	private final int maxGLBs;
	private final Map<Statement, Integer> glbs;

	public DescendingGLBFixpoint(CFG target, int maxGLBs,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(target, interprocedural);
		this.maxGLBs = maxGLBs;
		this.glbs = new HashMap<>(target.getNodesCount());
	}

	@Override
	public CompoundState<A, H, V, T> operation(Statement node,
			CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		AnalysisState<A, H, V, T> newApprox = approx.postState, oldApprox = old.postState;
		StatementStore<A, H, V, T> newIntermediate = approx.intermediateStates,
				oldIntermediate = old.intermediateStates;

		int glb = glbs.computeIfAbsent(node, st -> maxGLBs);
		if (glb > 0) {
			newApprox = newApprox.glb(oldApprox);
			newIntermediate = newIntermediate.glb(oldIntermediate);
		} else {
			newApprox = oldApprox;
			newIntermediate = oldIntermediate;
		}
		glbs.put(node, --glb);

		return CompoundState.of(newApprox, newIntermediate);
	}

	@Override
	public boolean equality(Statement node, CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		return old.postState.lessOrEqual(approx.postState)
				&& old.intermediateStates.lessOrEqual(approx.intermediateStates);
	}
}
