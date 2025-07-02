package it.unive.lisa.program.language.scoping;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A strategy for scoping around calls, i.e., converting the pre-state of a call
 * to a valid entry state for one of its targets, and converting state at return
 * points of a CFG to post-state of the call. Implementers have a degree of
 * freedom over what to scope: all variables, some variables, heap locations,
 * ...
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface ScopingStrategy {

	/**
	 * Converts the pre-state {@code state} of the given {@code call} to a valid
	 * entry state for one of its targets. Specifically, the state returned by
	 * this method corresponds to the given {@code state} modified by (i)
	 * pushing the scope that is introduced when the call happens (and that can
	 * be popped with {@link #unscope(CFGCall, ScopeToken, AnalysisState)}), and
	 * (ii) generating the expressions for the actual parameters by pushing the
	 * same scope to the ones of the actual parameters, starting from
	 * {@code actuals}.
	 * 
	 * @param <A>     the type of {@link AbstractState} used by this strategy
	 * @param call    the call that has to be performed
	 * @param scope   the scope corresponding to the call
	 * @param state   the exit state of the call's target
	 * @param actuals the expressions representing the actual parameters at the
	 *                    program point of the call
	 * 
	 * @return a pair containing the computed call state and the expressions
	 *             corresponding to the formal parameters
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	<A extends AbstractState<A>> Pair<AnalysisState<A>, ExpressionSet[]> scope(
			CFGCall call,
			ScopeToken scope,
			AnalysisState<A> state,
			ExpressionSet[] actuals)
			throws SemanticException;

	/**
	 * Converts the exit state of a cfg that was invoked by {@code call} to a
	 * valid post-state of {@code call}. Specifically, the state returned by
	 * this method corresponds to the parameter {@code state} modified by (i)
	 * popping the scope introduced before the call happened (see
	 * {@link #scope(CFGCall, ScopeToken, AnalysisState, ExpressionSet[])}), and (ii)
	 * storing the returned value on the meta-variable left on the stack, if
	 * any.
	 * 
	 * @param <A>   the type of {@link AbstractState} used by this strategy
	 * @param call  the call that caused the computation of the given state
	 *                  through a fixpoint computation
	 * @param scope the scope corresponding to the call
	 * @param state the exit state of the call's target
	 * 
	 * @return the state that can be returned by the call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	<A extends AbstractState<A>> AnalysisState<A> unscope(
			CFGCall call,
			ScopeToken scope,
			AnalysisState<A> state)
			throws SemanticException;
}
