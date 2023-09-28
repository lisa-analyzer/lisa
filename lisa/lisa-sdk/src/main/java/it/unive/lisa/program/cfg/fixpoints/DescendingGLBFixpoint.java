package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.conf.FixpointConfiguration;
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
 */
public class DescendingGLBFixpoint<A extends AbstractState<A>> extends CFGFixpoint<A> {

	private final int maxGLBs;
	private final Map<Statement, Integer> glbs;

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param target          the target of the implementation
	 * @param interprocedural the {@link InterproceduralAnalysis} to use for
	 *                            semantics computations
	 * @param config          the {@link FixpointConfiguration} to use
	 */
	public DescendingGLBFixpoint(
			CFG target,
			InterproceduralAnalysis<A> interprocedural,
			FixpointConfiguration config) {
		super(target, interprocedural);
		this.maxGLBs = config.glbThreshold;
		this.glbs = new HashMap<>(target.getNodesCount());
	}

	@Override
	public CompoundState<A> operation(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		if (maxGLBs < 0)
			return old;

		int glb = glbs.computeIfAbsent(node, st -> maxGLBs);
		if (glb == 0)
			return old;

		glbs.put(node, --glb);
		return old.glb(approx);
	}

	@Override
	public boolean equality(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		return old.lessOrEqual(approx);
	}
}
