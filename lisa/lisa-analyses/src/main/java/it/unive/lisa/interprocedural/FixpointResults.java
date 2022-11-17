package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.CFG;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link FunctionalLattice} from {@link CFG}s to {@link CFGResults}s. This
 * class is meant to store all fixpoint results on all token generated during
 * the interprocedural analysis for each cfg under analysis.
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
public class FixpointResults<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends FunctionalLattice<FixpointResults<A, H, V, T>, CFG, CFGResults<A, H, V, T>> {

	/**
	 * Builds a new result.
	 * 
	 * @param lattice a singleton instance used for retrieving top and bottom
	 *                    values
	 */
	public FixpointResults(CFGResults<A, H, V, T> lattice) {
		super(lattice);
	}

	private FixpointResults(CFGResults<A, H, V, T> lattice, Map<CFG, CFGResults<A, H, V, T>> function) {
		super(lattice, function);
	}

	/**
	 * Stores the result of a fixpoint computation on a cfg. This method returns
	 * the result of calling
	 * {@link CFGResults#putResult(ContextSensitivityToken, CFGWithAnalysisResults)}
	 * with the given {@code token} and {@code result} on the {@link CFGResults}
	 * instance corresponding to {@code cfg}.
	 * 
	 * @param cfg    the {@link CFG} on which the result has been computed
	 * @param token  the {@link ContextSensitivityToken} that identifying the
	 *                   result
	 * @param result the {@link CFGWithAnalysisResults} to store
	 * 
	 * @return the result of the update operation on the individual cfg result
	 * 
	 * @throws SemanticException if something goes wrong during the update
	 */
	public Pair<Boolean, CFGWithAnalysisResults<A, H, V, T>> putResult(CFG cfg, ContextSensitivityToken token,
			CFGWithAnalysisResults<A, H, V, T> result)
			throws SemanticException {
		if (function == null)
			function = mkNewFunction(null, false);
		CFGResults<A, H, V, T> res = function.computeIfAbsent(cfg, c -> new CFGResults<>(result.top()));
		return res.putResult(token, result);
	}

	/**
	 * Yields {@code true} if a result exists for the given {@code cfg}.
	 * 
	 * @param cfg the {@link CFG} whose result is to be checked
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean contains(CFG cfg) {
		return function != null && function.containsKey(cfg);
	}

	@Override
	public FixpointResults<A, H, V, T> top() {
		return new FixpointResults<>(lattice.top());
	}

	@Override
	public FixpointResults<A, H, V, T> bottom() {
		return new FixpointResults<>(lattice.bottom());
	}

	/**
	 * Forgets all results about the given {@link CFG}.
	 * 
	 * @param cfg the cfg to forget
	 */
	public void forget(CFG cfg) {
		if (function == null)
			return;
		function.remove(cfg);
		if (function.isEmpty())
			function = null;
	}

	@Override
	public FixpointResults<A, H, V, T> mk(CFGResults<A, H, V, T> lattice,
			Map<CFG, CFGResults<A, H, V, T>> function) {
		return new FixpointResults<>(lattice, function);
	}
}
