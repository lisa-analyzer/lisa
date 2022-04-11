package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
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
 * @param <H> the type of {@link HeapDomain} embedded in this state
 * @param <V> the type of {@link ValueDomain} embedded in this state
 * @param <T> the type of {@link ValueDomain} and {@link TypeDomain} embedded in
 *                this state
 */
public interface AbstractState<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends Lattice<A>, SemanticDomain<A, SymbolicExpression, Identifier> {

	public static final String HEAP_REPRESENTATION_KEY = "heap";
	public static final String TYPE_REPRESENTATION_KEY = "type";
	public static final String VALUE_REPRESENTATION_KEY = "value";

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

	/**
	 * Yields the instance of {@link ValueDomain} and {@link TypeDomain} that
	 * contains the information on runtime types of program variables and
	 * concretized memory locations.
	 * 
	 * @return the type domain
	 */
	T getTypeState();
}
