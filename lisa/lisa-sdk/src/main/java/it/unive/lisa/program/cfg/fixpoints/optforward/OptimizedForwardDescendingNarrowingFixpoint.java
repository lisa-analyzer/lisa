package it.unive.lisa.program.cfg.fixpoints.optforward;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingNarrowingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.optbackward.OptimizedBackwardDescendingNarrowingFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * An {@link OptimizedForwardFixpoint} that traverses descending chains using
 * narrowings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class OptimizedForwardDescendingNarrowingFixpoint<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		OptimizedForwardFixpoint<A, D> {

	private final FixpointConfiguration<A, D> config;

	private final Collection<Statement> wideningPoints;

	/**
	 * Builds the fixpoint implementation. Note that the implementation built
	 * with this constructor is inherently invalid, as it does not target any
	 * cfg and has no information on the analysis to run. Valid instances should
	 * be built throug the
	 * {@link #OptimizedForwardDescendingNarrowingFixpoint(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration, Predicate)}
	 * constructor or the
	 * {@link #mk(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * method. Invocations of the latter will preserve the hotspots predicate.
	 */
	public OptimizedForwardDescendingNarrowingFixpoint() {
		super(null, false, null, null, null);
		this.config = null;
		this.wideningPoints = null;
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
	 * @param events              the event queue to use to emit analysis events
	 * @param hotspots            the predicate to identify additional
	 *                                statements whose approximation must be
	 *                                preserved in the results
	 */
	public OptimizedForwardDescendingNarrowingFixpoint(
			CFG target,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration<A, D> config,
			EventQueue events,
			Predicate<Statement> hotspots) {
		super(target, forceFullEvaluation, interprocedural, events, hotspots);
		this.config = config;
		this.wideningPoints = config.useWideningPoints ? target.getCycleEntries() : null;
	}

	@Override
	public CompoundState<A> join(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		if (wideningPoints == null || !wideningPoints.contains(node))
			// optimization: never apply narrowing on normal instructions,
			// save time and precision and only apply to widening points
			return old.downchain(approx);

		AnalysisState<A> post = old.postState.narrowing(approx.postState);
		StatementStore<A> intermediate;
		if (config.useWideningPoints)
			// no need to narrow the intermediate expressions as
			// well: we force convergence on the final post state
			// only, to recover as much precision as possible
			intermediate = old.intermediateStates.downchain(approx.intermediateStates);
		else
			intermediate = old.intermediateStates.narrowing(approx.intermediateStates);
		return CompoundState.of(post, intermediate);
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
	public ForwardCFGFixpoint<A, D> mk(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration<A, D> config) {
		return new OptimizedForwardDescendingNarrowingFixpoint<>(
				graph,
				forceFullEvaluation,
				interprocedural,
				config,
				events,
				hotspots);
	}

	@Override
	public ForwardCFGFixpoint<A, D> asUnoptimized() {
		return new ForwardDescendingNarrowingFixpoint<>();
	}

	@Override
	public BackwardCFGFixpoint<A, D> asBackward() {
		return new OptimizedBackwardDescendingNarrowingFixpoint<>();
	}

	@Override
	public ForwardCFGFixpoint<A, D> withHotspots(
			Predicate<Statement> hotspots) {
		return new OptimizedForwardDescendingNarrowingFixpoint<>(
				graph,
				forceFullEvaluation,
				interprocedural,
				config,
				events,
				hotspots);
	}

}
