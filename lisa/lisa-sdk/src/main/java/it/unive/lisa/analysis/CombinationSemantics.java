package it.unive.lisa.analysis;

import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * The semantics of a statement on one of the combinations of the expressions
 * computed by its operands (see
 * {@link Analysis#ofCombination(AnalysisState, ScopeId, ProgramPoint, ProgramPoint[], SymbolicExpression[], CombinationSemantics)}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} of the states
 */
@FunctionalInterface
public interface CombinationSemantics<A extends AbstractLattice<A>> {

	/**
	 * Computes the semantics in the given executions (that the analysis might
	 * have restricted to the ones computing the combination).
	 *
	 * @param state the executions where the semantics is computed
	 *
	 * @return the state after the semantics
	 *
	 * @throws SemanticException if an error occurs during the computation
	 */
	AnalysisState<A> compute(
			AnalysisState<A> state)
			throws SemanticException;
}
