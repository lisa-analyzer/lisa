package it.unive.lisa.program.cfg.fixpoints.backward;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.AnalysisFixpoint;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardAscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.fixpoints.optbackward.OptimizedBackwardAscendingFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A {@link BackwardCFGFixpoint} that traverses ascending chains using lubs and
 * widenings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class BackwardAscendingFixpoint<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		BackwardCFGFixpoint<A, D> {

	private final FixpointConfiguration config;

	private final Map<Statement, Integer> lubs;

	private final Collection<Statement> wideningPoints;

	/**
	 * Builds the fixpoint implementation. Note that the implementation built
	 * with this constructor is inherently invalid, as it does not target any
	 * cfg and has no information on the analysis to run. Valid instances should
	 * be built throug the
	 * {@link #BackwardAscendingFixpoint(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * constructor or the
	 * {@link #mk(CFG, boolean, InterproceduralAnalysis, FixpointConfiguration)}
	 * method.
	 */
	public BackwardAscendingFixpoint() {
		this(null, false, null, null);
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
	public BackwardAscendingFixpoint(
			CFG target,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration config) {
		super(target, forceFullEvaluation, interprocedural);
		this.config = config;
		this.wideningPoints = config.useWideningPoints ? target.getCycleEntries() : null;
		this.lubs = new HashMap<>(config.useWideningPoints ? wideningPoints.size() : target.getNodesCount());
	}

	@Override
	public CompoundState<A> join(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		if (config.wideningThreshold < 0)
			// invalid threshold means always lub
			return old.lub(approx);

		if (config.useWideningPoints && !wideningPoints.contains(node))
			// optimization: never apply widening on normal instructions,
			// save time and precision and only apply to widening points
			return old.lub(approx);

		int lub = lubs.computeIfAbsent(node, st -> config.wideningThreshold);
		if (lub == 0) {
			AnalysisState<A> post = old.postState.widening(approx.postState);
			StatementStore<A> intermediate;
			if (config.useWideningPoints)
				// no need to widen the intermediate expressions as
				// well: we force convergence on the final post state
				// only, to recover as much precision as possible
				intermediate = old.intermediateStates.lub(approx.intermediateStates);
			else
				intermediate = old.intermediateStates.widening(approx.intermediateStates);
			return CompoundState.of(post, intermediate);
		}

		lubs.put(node, --lub);
		return old.lub(approx);
	}

	@Override
	public boolean leq(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		return approx.lessOrEqual(old);
	}

	@Override
	public BackwardCFGFixpoint<A, D> mk(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			FixpointConfiguration config) {
		return new BackwardAscendingFixpoint<>(graph, forceFullEvaluation, interprocedural, config);
	}

	@Override
	public AnalysisFixpoint<?, A, D> asOptimized() {
		return new OptimizedBackwardAscendingFixpoint<>();
	}

	@Override
	public ForwardCFGFixpoint<A, D> asForward() {
		return new ForwardAscendingFixpoint<>();
	}

}
