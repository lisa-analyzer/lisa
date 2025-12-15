package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint;

/**
 * A generic interface for analysis fixpoints that enables the creation of new
 * instances with the same configuration through
 * {@link #mk(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <F> the concrete type of fixpoint type
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public interface AnalysisFixpoint<
		F extends AnalysisFixpoint<F, A, D>,
		A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		Fixpoint<CFG, Statement, Edge, CompoundState<A>> {

	/**
	 * Builds a fixpoint for the given {@link CFG}.
	 * 
	 * @param graph               the source cfg
	 * @param forceFullEvaluation whether or not the fixpoint should evaluate
	 *                                all nodes independently of the fixpoint
	 *                                implementation
	 * @param interprocedural     the {@link InterproceduralAnalysis} to use for
	 *                                semantics computations
	 * @param config              the {@link FixpointConfiguration} to use
	 * 
	 * @return the fixpoint instance
	 */
	F mk(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration config);

	/**
	 * Yields whether or not this fixpoint is optimized.
	 * 
	 * @return {@code true} if this fixpoint is optimized, {@code false}
	 *             otherwise
	 */
	boolean isOptimized();

	/**
	 * Yields an optimized version of this fixpoint. If this fixpoint is already
	 * optimized, this method returns {@code this}.
	 * 
	 * @return the optimized fixpoint
	 */
	AnalysisFixpoint<?, A, D> asOptimized();

	/**
	 * Yields an unoptimized version of this fixpoint. If this fixpoint is
	 * already unoptimized, this method returns {@code this}.
	 * 
	 * @return the unoptimized fixpoint
	 */
	AnalysisFixpoint<?, A, D> asUnoptimized();

	/**
	 * Yields a forward version of this fixpoint. If this fixpoint is already
	 * forward, this method returns {@code this}.
	 * 
	 * @return the forward fixpoint
	 */
	ForwardCFGFixpoint<A, D> asForward();

	/**
	 * Yields a backward version of this fixpoint. If this fixpoint is already
	 * backward, this method returns {@code this}.
	 * 
	 * @return the backward fixpoint
	 */
	BackwardCFGFixpoint<A, D> asBackward();
}
