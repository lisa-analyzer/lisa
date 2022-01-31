package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * Policy that determines what happens to the {@link AnalysisState} when an
 * {@link OpenCall} is encountered during the fixpoint. The state is directly
 * transformed by this policy.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface OpenCallPolicy {

	/**
	 * The name of the variable storing the return value of the call, if any.
	 */
	public static final String RETURNED_VARIABLE_NAME = "open_call_return";

	/**
	 * Applies the policy to the given open call.
	 * 
	 * @param <A>        the type of {@link AbstractState} contained into the
	 *                       analysis state
	 * @param <H>        the type of {@link HeapDomain} contained into the
	 *                       computed abstract state
	 * @param <V>        the type of {@link ValueDomain} contained into the
	 *                       computed abstract state
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
	<A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> apply(
					OpenCall call,
					AnalysisState<A, H, V, T> entryState,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException;
}
