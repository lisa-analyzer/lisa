package it.unive.lisa.program.cfg.fixpoints.backward;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.events.JoinPerformed;
import it.unive.lisa.program.cfg.fixpoints.events.LeqPerformed;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingGLBFixpoint;
import it.unive.lisa.program.cfg.fixpoints.optbackward.OptimizedBackwardDescendingGLBFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link BackwardCFGFixpoint} that traverses descending chains using glbs up
 * to threshold.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class BackwardDescendingGLBFixpoint<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		BackwardCFGFixpoint<A, D> {

	private final int maxGLBs;

	private final Map<Statement, Integer> glbs;

	/**
	 * Builds the fixpoint implementation. Note that the implementation built
	 * with this constructor is inherently invalid, as it does not target any
	 * cfg and has no information on the analysis to run. Valid instances should
	 * be built throug the
	 * {@link #BackwardDescendingGLBFixpoint(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * constructor or the
	 * {@link #mk(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * method.
	 */
	public BackwardDescendingGLBFixpoint() {
		super(null, false, null);
		this.maxGLBs = -1;
		this.glbs = null;
	}

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param target              the target of the implementation
	 * @param forceFullEvaluation whether or not the fixpoint should evaluate
	 *                                all nodes independently of the fixpoint
	 *                                implementation
	 * @param interprocedural     the {@link InterproceduralAnalysis} to use for
	 *                                semantics computations
	 * @param config              the {@link FixpointConfiguration} to use
	 */
	public BackwardDescendingGLBFixpoint(
			CFG target,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration<A, D> config) {
		super(target, forceFullEvaluation, interprocedural);
		this.maxGLBs = config.glbThreshold;
		this.glbs = new HashMap<>(target.getNodesCount());
	}

	@Override
	public CompoundState<A> join(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		CompoundState<A> result;
		if (maxGLBs < 0)
			result = old;
		else {
			int glb = glbs.computeIfAbsent(node, st -> maxGLBs);
			if (glb == 0)
				result = old;
			else {
				glbs.put(node, --glb);
				result = old.downchain(approx);
			}
		}

		if (events != null)
			events.post(new JoinPerformed<>(node, old, approx, result));

		return result;
	}

	@Override
	public boolean leq(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		boolean result = old.lessOrEqual(approx);
		if (events != null)
			events.post(new LeqPerformed<A>(node, old, approx, result));
		return result;
	}

	@Override
	public BackwardCFGFixpoint<A, D> mk(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration<A, D> config) {
		return new BackwardDescendingGLBFixpoint<>(graph, forceFullEvaluation, interprocedural, config);
	}

	@Override
	public BackwardCFGFixpoint<A, D> asOptimized() {
		return new OptimizedBackwardDescendingGLBFixpoint<>();
	}

	@Override
	public ForwardCFGFixpoint<A, D> asForward() {
		return new ForwardDescendingGLBFixpoint<>();
	}

}
