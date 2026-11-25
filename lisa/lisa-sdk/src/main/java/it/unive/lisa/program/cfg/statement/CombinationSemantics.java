package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;

/**
 * The semantics of a statement on one of the combinations of the expressions
 * computed by its operands, evaluated in the executions computing that
 * combination (see
 * {@link it.unive.lisa.analysis.Analysis#ofCombination(AnalysisState, it.unive.lisa.interprocedural.ScopeId, it.unive.lisa.program.cfg.ProgramPoint, it.unive.lisa.program.cfg.ProgramPoint[], it.unive.lisa.symbolic.SymbolicExpression[], CombinationSemantics)}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} of the states
 */
@FunctionalInterface
public interface CombinationSemantics<A extends AbstractLattice<A>> {

	/**
	 * Computes the semantics in the given executions, the ones computing the
	 * combination.
	 *
	 * @param restricted the executions computing the combination
	 *
	 * @return the state after the semantics
	 *
	 * @throws SemanticException if an error occurs during the computation
	 */
	AnalysisState<A> compute(
			AnalysisState<A> restricted)
			throws SemanticException;
}
