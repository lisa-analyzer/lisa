package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
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
 */
public class FixpointResults<A extends AbstractState<A>>
		extends FunctionalLattice<FixpointResults<A>, CFG, CFGResults<A>> {

	/**
	 * Builds a new result.
	 * 
	 * @param lattice a singleton instance used for retrieving top and bottom
	 *                    values
	 */
	public FixpointResults(CFGResults<A> lattice) {
		super(lattice);
	}

	private FixpointResults(CFGResults<A> lattice, Map<CFG, CFGResults<A>> function) {
		super(lattice, function);
	}

	/**
	 * Stores the result of a fixpoint computation on a cfg. This method returns
	 * the result of calling {@link CFGResults#putResult(ScopeId, AnalyzedCFG)}
	 * with the given {@code token} and {@code result} on the {@link CFGResults}
	 * instance corresponding to {@code cfg}.
	 * 
	 * @param cfg    the {@link CFG} on which the result has been computed
	 * @param token  the {@link ScopeId} that identifying the result
	 * @param result the {@link AnalyzedCFG} to store
	 * 
	 * @return the result of the update operation on the individual cfg result
	 * 
	 * @throws SemanticException if something goes wrong during the update
	 */
	public Pair<Boolean, AnalyzedCFG<A>> putResult(CFG cfg, ScopeId token,
			AnalyzedCFG<A> result)
			throws SemanticException {
		if (function == null)
			function = mkNewFunction(null, false);
		CFGResults<A> res = function.computeIfAbsent(cfg, c -> new CFGResults<>(result.top()));
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

	/**
	 * Yields the recorded result for the given {@code cfg}. This differs from
	 * {@link #getState(Object)} as it returns {@code null} instead of
	 * {@link Lattice#bottom()} if there is no recorded result for the given
	 * cfg.
	 * 
	 * @param cfg the {@link CFG} whose result is to be checked
	 * 
	 * @return the result, or {@code null}
	 */
	public CFGResults<A> get(CFG cfg) {
		return function == null ? null : function.get(cfg);
	}

	@Override
	public FixpointResults<A> top() {
		return new FixpointResults<>(lattice.top());
	}

	@Override
	public FixpointResults<A> bottom() {
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
	public FixpointResults<A> mk(CFGResults<A> lattice,
			Map<CFG, CFGResults<A>> function) {
		return new FixpointResults<>(lattice, function);
	}
}
