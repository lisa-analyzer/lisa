package it.unive.lisa.analysis;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.program.cfg.ProgramPoint;
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
		extends
		SemanticOracle,
		Lattice<A>,
		SemanticDomain<A, SymbolicExpression, Identifier> {

	/**
	 * Yields a copy of this state, but with its memory abstraction set to top.
	 * This is useful to represent effects of unknown calls that arbitrarily
	 * manipulate the memory.
	 * 
	 * @return the copy with top memory
	 */
	A withTopMemory();

	/**
	 * Yields a copy of this state, but with its value abstraction set to top.
	 * This is useful to represent effects of unknown calls that arbitrarily
	 * manipulate the values of variables.
	 * 
	 * @return the copy with top values
	 */
	A withTopValues();

	/**
	 * Yields a copy of this state, but with its type abstraction set to top.
	 * This is useful to represent effects of unknown calls that arbitrarily
	 * manipulate the values of variables (and their type accordingly).
	 * 
	 * @return the copy with top types
	 */
	A withTopTypes();
	
	Pair<A, A> split(SymbolicExpression expr, ProgramPoint src,
			ProgramPoint dest, SemanticOracle oracle);
}
