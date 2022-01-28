package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ImplementedCFG;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link FunctionalLattice} from {@link ImplementedCFG}s to
 * {@link CFGResults}s. This class is meant to store all fixpoint results on all
 * token generated during the interprocedural analysis for each cfg under
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 */
public class FixpointResults<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>>
		extends FunctionalLattice<FixpointResults<A, H, V>, ImplementedCFG, CFGResults<A, H, V>> {

	/**
	 * Builds a new result.
	 * 
	 * @param lattice a singleton instance used for retrieving top and bottom
	 *                    values
	 */
	public FixpointResults(CFGResults<A, H, V> lattice) {
		super(lattice);
	}

	private FixpointResults(CFGResults<A, H, V> lattice, Map<ImplementedCFG, CFGResults<A, H, V>> function) {
		super(lattice, function);
	}

	/**
	 * Stores the result of a fixpoint computation on a cfg. This method returns
	 * the result of calling
	 * {@link CFGResults#putResult(ContextSensitivityToken, CFGWithAnalysisResults)}
	 * with the given {@code token} and {@code result} on the {@link CFGResults}
	 * instance corresponding to {@code cfg}.
	 * 
	 * @param cfg    the {@link ImplementedCFG} on which the result has been
	 *                   computed
	 * @param token  the {@link ContextSensitivityToken} that identifying the
	 *                   result
	 * @param result the {@link CFGWithAnalysisResults} to store
	 * 
	 * @return the result of the update operation on the individual cfg result
	 * 
	 * @throws SemanticException if something goes wrong during the update
	 */
	public Pair<Boolean, CFGWithAnalysisResults<A, H, V>> putResult(ImplementedCFG cfg, ContextSensitivityToken token,
			CFGWithAnalysisResults<A, H, V> result)
			throws SemanticException {
		CFGResults<A, H, V> res = function.computeIfAbsent(cfg, c -> new CFGResults<>(result.top()));
		return res.putResult(token, result);
	}

	/**
	 * Yields {@code true} if a result exists for the given {@code cfg}.
	 * 
	 * @param cfg the {@link ImplementedCFG} whose result is to be checked
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean contains(ImplementedCFG cfg) {
		return function != null && function.containsKey(cfg);
	}

	@Override
	public FixpointResults<A, H, V> top() {
		return new FixpointResults<>(lattice.top());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && (function == null || function.isEmpty());
	}

	@Override
	public FixpointResults<A, H, V> bottom() {
		return new FixpointResults<>(lattice.bottom());
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && (function == null || function.isEmpty());
	}

	/**
	 * Forgets all results about the given {@link ImplementedCFG}.
	 * 
	 * @param cfg the cfg to forget
	 */
	public void forget(ImplementedCFG cfg) {
		function.remove(cfg);
	}

	@Override
	protected FixpointResults<A, H, V> mk(CFGResults<A, H, V> lattice,
			Map<ImplementedCFG, CFGResults<A, H, V>> function) {
		return new FixpointResults<>(lattice, function);
	}
}
