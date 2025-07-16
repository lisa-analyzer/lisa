package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
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
	 * @param <A>        the kind of {@link AbstractLattice} produced by the
	 *                       domain {@code D}
	 * @param <D>        the kind of {@link AbstractDomain} to run during the
	 *                       analysis
	 * @param call       the {@link OpenCall} under evaluation
	 * @param entryState the state when the call is executed
	 * @param analysis   the analysis that is being executed
	 * @param params     the symbolic expressions representing the computed
	 *                       values of the parameters of the call
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	<A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> apply(
					OpenCall call,
					AnalysisState<A> entryState,
					Analysis<A, D> analysis,
					ExpressionSet[] params)
					throws SemanticException;

}
