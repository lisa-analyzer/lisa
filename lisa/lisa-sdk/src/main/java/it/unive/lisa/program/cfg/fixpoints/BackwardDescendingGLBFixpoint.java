package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class BackwardDescendingGLBFixpoint<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		BackwardCFGFixpoint<A, D> {

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
	public BackwardDescendingGLBFixpoint(
			CFG target,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration config) {
		super(target, interprocedural);
		this.maxGLBs = config.glbThreshold;
		this.glbs = new HashMap<>(target.getNodesCount());
	}

	@Override
	public CompoundState<A> join(
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
	public boolean leq(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		return old.lessOrEqual(approx);
	}

}
