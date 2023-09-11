package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.call.OpenCall;

/**
 * Policy that determines what happens to the {@link AnalysisState} when an
 * {@link OpenCall} is encountered during the fixpoint. The state is directly
 * transformed by this policy.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface OpenCallPolicy {

	/**
	 * Applies the policy to the given open call.
	 * 
	 * @param <A>        the type of {@link AbstractState} contained into the
	 *                       analysis state
	 * @param call       the {@link OpenCall} under evaluation
	 * @param entryState the state when the call is executed
	 * @param params     the symbolic expressions representing the computed
	 *                       values of the parameters of the call
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	<A extends AbstractState<A>> AnalysisState<A> apply(
			OpenCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] params)
			throws SemanticException;
}
