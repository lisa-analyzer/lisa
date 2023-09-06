package it.unive.lisa.program.cfg.fixpoints;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link CFGFixpoint} that traverses ascending chains using lubs and
 * widenings.
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
public class AscendingFixpoint<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends CFGFixpoint<A, H, V, T> {

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
	public AscendingFixpoint(CFG target,
			InterproceduralAnalysis<A, H, V, T> interprocedural,
			FixpointConfiguration config) {
		super(target, interprocedural);
		this.config = config;
		this.wideningPoints = config.useWideningPoints ? target.getCycleEntries() : null;
		this.lubs = new HashMap<>(config.useWideningPoints ? wideningPoints.size() : target.getNodesCount());
	}

	@Override
	public CompoundState<A, H, V, T> operation(Statement node,
			CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		if (config.wideningThreshold < 0)
			// invalid threshold means always lub
			return old.lub(approx);

		if (config.useWideningPoints && !wideningPoints.contains(node))
			// optimization: never apply widening on normal instructions,
			// save time and precision and only apply to widening points
			return old.lub(approx);

		int lub = lubs.computeIfAbsent(node, st -> config.wideningThreshold);
		if (lub == 0) {
			AnalysisState<A, H, V, T> post = old.postState.widening(approx.postState);
			StatementStore<A, H, V, T> intermediate;
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
	public boolean equality(Statement node, CompoundState<A, H, V, T> approx,
			CompoundState<A, H, V, T> old) throws SemanticException {
		return approx.lessOrEqual(old);
	}
}
