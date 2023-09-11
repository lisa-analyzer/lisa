package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link FunctionalLattice} from {@link ScopeId}s to {@link AnalyzedCFG}s.
 * This class is meant to store fixpoint results on each token generated during
 * the interprocedural analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 */
public class CFGResults<A extends AbstractState<A>>
		extends FunctionalLattice<CFGResults<A>, ScopeId, AnalyzedCFG<A>> {

	/**
	 * Builds a new result.
	 * 
	 * @param lattice a singleton instance used for retrieving top and bottom
	 *                    values
	 */
	public CFGResults(AnalyzedCFG<A> lattice) {
		super(lattice);
	}

	private CFGResults(AnalyzedCFG<A> lattice,
			Map<ScopeId, AnalyzedCFG<A>> function) {
		super(lattice, function);
	}

	/**
	 * Stores the result of a fixpoint computation on a cfg, if needed. This
	 * method returns a pair of a boolean and a {@link AnalyzedCFG}, where
	 * ({@code prev} is the {@link AnalyzedCFG} already present for the given
	 * {@code token}):
	 * <ul>
	 * <li>if no {@code prev} was stored for {@code token}, than that token is
	 * mapped to {@code result} and this method returns
	 * {@code <false, result>}</li>
	 * <li>if {@code prev <= result}, then {@code token} is mapped to
	 * {@code result} and this method returns {@code <true, result>}</li>
	 * <li>if {@code result <= prev}, then the mapping is left untouched and
	 * this method returns {@code <false, prev>}</li>
	 * <li>otherwise, {@code token} is mapped to {@code lub = prev.lub(result)}
	 * and this method returns {@code <true, lub>}</li>
	 * </ul>
	 * The value returned by this method is intended to be a hint that a new
	 * fixpoint computation is needed to ensure that the results are stable.
	 * 
	 * @param token  the {@link ScopeId} that identifying the result
	 * @param result the {@link AnalyzedCFG} to store
	 * 
	 * @return {@code true} if the previous result has been updated, if any
	 * 
	 * @throws SemanticException if something goes wrong during the update
	 */
	public Pair<Boolean, AnalyzedCFG<A>> putResult(ScopeId token,
			AnalyzedCFG<A> result)
			throws SemanticException {
		if (function == null) {
			// no previous result
			function = mkNewFunction(null, false);
			function.put(token, result);
			return Pair.of(false, result);
		}

		AnalyzedCFG<A> previousResult = function.get(token);
		if (previousResult == null) {
			// no previous result
			function.put(token, result);
			return Pair.of(false, result);
		} else if (previousResult.lessOrEqual(result)) {
			// previous is smaller than result
			if (result.lessOrEqual(previousResult))
				// they are equal
				return Pair.of(false, previousResult);
			else {
				// result is bigger, store that instead
				function.put(token, result);
				return Pair.of(true, result);
			}
		} else if (result.lessOrEqual(previousResult)) {
			// result is smaller than previous
			return Pair.of(false, previousResult);
		} else {
			// result and previous are not comparable
			AnalyzedCFG<A> lub = previousResult.lub(result);
			function.put(token, lub);
			return Pair.of(true, lub);
		}
	}

	/**
	 * Yields {@code true} if a result exists for the given {@code token}.
	 * 
	 * @param token the {@link ScopeId} that identifying the result
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean contains(ScopeId token) {
		return function != null && function.containsKey(token);
	}

	/**
	 * Yields the recorded result for the given {@code token}. This differs from
	 * {@link #getState(Object)} as it returns {@code null} instead of
	 * {@link Lattice#bottom()} if there is no recorded result for the given
	 * token.
	 * 
	 * @param token the {@link ScopeId} that identifying the result
	 * 
	 * @return the result, or {@code null}
	 */
	public AnalyzedCFG<A> get(ScopeId token) {
		return function == null ? null : function.get(token);
	}

	/**
	 * Yields all the results stored in this object, for any possible
	 * {@link ScopeId} used.
	 * 
	 * @return the results
	 */
	public Collection<AnalyzedCFG<A>> getAll() {
		return function == null ? Collections.emptySet() : function.values();
	}

	@Override
	public CFGResults<A> top() {
		return new CFGResults<>(lattice.top());
	}

	@Override
	public CFGResults<A> bottom() {
		return new CFGResults<>(lattice.bottom());
	}

	@Override
	public CFGResults<A> mk(AnalyzedCFG<A> lattice,
			Map<ScopeId, AnalyzedCFG<A>> function) {
		return new CFGResults<>(lattice, function);
	}

	@Override
	public AnalyzedCFG<A> stateOfUnknown(ScopeId key) {
		return lattice.bottom();
	}
}
