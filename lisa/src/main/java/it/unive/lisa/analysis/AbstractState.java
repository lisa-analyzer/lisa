package it.unive.lisa.analysis;

import it.unive.lisa.DefaultImplementation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout and a value state modeling values of program variables and
 * memory locations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the concrete type of the {@link AbstractState}
 * @param <H> the type of {@link HeapDomain} embedded in this state
 * @param <V> the type of {@link ValueDomain} embedded in this state
 */
@DefaultImplementation(SimpleAbstractState.class)
public interface AbstractState<A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>>
		extends Lattice<A>, SemanticDomain<A, SymbolicExpression, Identifier> {

	/**
	 * Yields the instance of {@link HeapDomain} that contains the information
	 * on heap structures contained in this abstract state.
	 * 
	 * @return the heap domain
	 */
	H getHeapState();

	/**
	 * Yields the instance of {@link ValueDomain} that contains the information
	 * on values of program variables and concretized memory locations.
	 * 
	 * @return the value domain
	 */
	V getValueState();
}
