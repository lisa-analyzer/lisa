package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link CFGFixpoint} that traverses descending chains using glbs up to
 * threshold.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 * @param <T> the type of {@link TypeDomain} contained into the computed
 *                abstract state
 */
public class DescendingGLBFixpoint<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends CFGFixpoint<A, H, V, T> {

	private final int maxGLBs;
	private final Map<Statement, Integer> glbs;

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param target          the target of the implementation
	 * @param maxGLBs         the maximum number of glbs
	 * @param interprocedural the {@link InterproceduralAnalysis} to use for
	 *                            semantics computations
	 */
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
		CompoundState<A, H, V, T> result;

		int glb = glbs.computeIfAbsent(node, st -> maxGLBs);
		if (glb > 0)
			result = old.glb(approx);
		else
			result = old;
		glbs.put(node, --glb);

		return result;
	}

	@Override
	public boolean equality(Statement node, CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		return old.lessOrEqual(approx);
	}
}
