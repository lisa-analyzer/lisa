package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout and a value state modeling values of program variables and
 * memory locations. An abstract state also wraps a domain to reason about
 * runtime types of such variables.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the concrete type of the {@link AbstractState}
 */
public interface AbstractState<A extends AbstractState<A>>
		extends SemanticOracle, Lattice<A>, SemanticDomain<A, SymbolicExpression, Identifier> {
}
