package it.unive.lisa.program.cfg.fixpoints;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A {@link CFGFixpoint} that traverses ascending chains using lubs and
 * widenings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 */
public class BackwardAscendingFixpoint<A extends AbstractState<A>>
		extends
		BackwardCFGFixpoint<A> {

	private final FixpointConfiguration config;
	private final Map<Statement, Integer> lubs;
	private final Collection<Statement> wideningPoints;

	/**
	 * Builds the fixpoint implementation.
	 * 
	 * @param target          the target of the implementation
	 * @param interprocedural the {@link InterproceduralAnalysis} to use for
	 *                            semantics computations
	 * @param config          the {@link FixpointConfiguration} to use
	 */
	public BackwardAscendingFixpoint(
			CFG target,
			InterproceduralAnalysis<A> interprocedural,
			FixpointConfiguration config) {
		super(target, interprocedural);
		this.config = config;
		this.wideningPoints = config.useWideningPoints ? target.getCycleEntries() : null;
		this.lubs = new HashMap<>(config.useWideningPoints ? wideningPoints.size() : target.getNodesCount());
	}

	@Override
	public CompoundState<A> operation(
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
	public boolean equality(
			Statement node,
			CompoundState<A> approx,
			CompoundState<A> old)
			throws SemanticException {
		return approx.lessOrEqual(old);
	}
}
