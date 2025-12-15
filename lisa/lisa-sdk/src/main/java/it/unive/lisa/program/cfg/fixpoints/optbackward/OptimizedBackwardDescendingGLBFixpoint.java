package it.unive.lisa.program.cfg.fixpoints.optbackward;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.AnalysisFixpoint;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardDescendingGLBFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.optforward.OptimizedForwardDescendingGLBFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An {@link OptimizedBackwardFixpoint} that traverses descending chains using
 * glbs up to threshold.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class OptimizedBackwardDescendingGLBFixpoint<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		OptimizedBackwardFixpoint<A, D> {

	private final int maxGLBs;

	private final Map<Statement, Integer> glbs;

	/**
	 * Builds the fixpoint implementation. Note that the implementation built
	 * with this constructor is inherently invalid, as it does not target any
	 * cfg and has no information on the analysis to run. Valid instances should
	 * be built throug the
	 * {@link #OptimizedBackwardDescendingGLBFixpoint(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * constructor or the
	 * {@link #mk(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * method. Invocations of the latter will preserve the hotspots predicate.
	 */
	public OptimizedBackwardDescendingGLBFixpoint() {
		this(null, false, null, null, null);
	}

	/**
	 * Builds the fixpoint implementation. Note that the implementation built
	 * with this constructor is inherently invalid, as it does not target any
	 * cfg and has no information on the analysis to run. Valid instances should
	 * be built throug the
	 * {@link #OptimizedBackwardDescendingGLBFixpoint(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * constructor or the
	 * {@link #mk(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * method. Invocations of the latter will preserve the hotspots predicate.
	 * 
	 * @param hotspots the predicate to identify additional statements (also
	 *                     considering intermediate ones) for which the fixpoint
	 *                     results must be kept. This is useful for avoiding
	 *                     result unwinding due to {@link SemanticCheck}s
	 *                     querying for the post-state of statements. Note that
	 *                     statements for which
	 *                     {@link Statement#stopsExecution()} is {@code true}
	 *                     are always considered hotspots
	 */
	public OptimizedBackwardDescendingGLBFixpoint(
			Predicate<Statement> hotspots) {
		this(null, false, null, null, hotspots);
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
	 * @param hotspots            the predicate to identify additional
	 *                                statements whose approximation must be
	 *                                preserved in the results
	 */
	public OptimizedBackwardDescendingGLBFixpoint(
			CFG target,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration config,
			Predicate<Statement> hotspots) {
		super(target, forceFullEvaluation, interprocedural, hotspots);
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

	@Override
	public BackwardCFGFixpoint<A, D> mk(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration config) {
		return new OptimizedBackwardDescendingGLBFixpoint<>(graph, forceFullEvaluation, interprocedural, config,
				hotspots);
	}

	@Override
	public AnalysisFixpoint<?, A, D> asUnoptimized() {
		return new BackwardDescendingGLBFixpoint<>();
	}

	@Override
	public ForwardCFGFixpoint<A, D> asForward() {
		return new OptimizedForwardDescendingGLBFixpoint<>(hotspots);
	}

}
