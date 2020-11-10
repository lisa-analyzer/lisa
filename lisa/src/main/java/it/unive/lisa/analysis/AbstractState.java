package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.Identifier;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapIdentifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.ValueIdentifier;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout and a value state modeling values of program variables and
 * memory locations.
 * 
 * @param <H> the type of {@link HeapDomain} embedded in this state
 * @param <V> the type of {@link ValueDomain} embedded in this state
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AbstractState<H extends HeapDomain<H>, V extends ValueDomain<V>>
		implements Lattice<AbstractState<H, V>>, SemanticDomain<AbstractState<H, V>, SymbolicExpression, Identifier> {

	/**
	 * The domain containing information regarding heap structures
	 */
	private final H heapState;

	/**
	 * The domain containing information regarding values of program variables and
	 * concretized memory locations
	 */
	private final V valueState;

	/**
	 * Builds a new abstract state.
	 * 
	 * @param heapState  the domain containing information regarding heap structures
	 * @param valueState the domain containing information regarding values of
	 *                   program variables and concretized memory locations
	 */
	public AbstractState(H heapState, V valueState) {
		this.heapState = heapState;
		this.valueState = valueState;
	}

	/**
	 * Yields the instance of {@link HeapDomain} that contains the information on
	 * heap structures contained in this abstract state.
	 * 
	 * @return the heap domain
	 */
	public H getHeapState() {
		return heapState;
	}

	/**
	 * Yields the instance of {@link ValueDomain} that contains the information on
	 * values of program variables and concretized memory locations
	 * 
	 * @return the value domain
	 */
	public V getValueState() {
		return valueState;
	}

	@Override
	public AbstractState<H, V> assign(Identifier id, SymbolicExpression value) {
		H heap = heapState;
		V val = valueState;

		if (id instanceof ValueIdentifier)
			val = val.assign((ValueIdentifier) id, (ValueExpression) value);
		else
			// TODO we should take care of replacements
			heap = heap.assign((HeapIdentifier) id, (HeapExpression) value);

		return new AbstractState<>(heap, val);
	}

	@Override
	public AbstractState<H, V> smallStepSemantics(SymbolicExpression expression) {
		H heap = heapState;
		V val = valueState;

		if (expression instanceof ValueExpression)
			val = val.smallStepSemantics((ValueExpression) expression);
		else
			// TODO we should take care of replacements
			heap = heap.smallStepSemantics((HeapExpression) expression);

		return new AbstractState<>(heap, val);
	}

	@Override
	public AbstractState<H, V> assume(SymbolicExpression expression) {
		H heap = heapState;
		V val = valueState;

		if (expression instanceof ValueExpression)
			val = val.assume((ValueExpression) expression);
		else
			// TODO we should take care of replacements
			heap = heap.assume((HeapExpression) expression);

		return new AbstractState<>(heap, val);
	}

	@Override
	public Satisfiability satisfy(SymbolicExpression expression) {
		if (expression instanceof ValueExpression)
			return valueState.satisfy((ValueExpression) expression);
		else
			return heapState.satisfy((HeapExpression) expression);
	}

	@Override
	public AbstractState<H, V> lub(AbstractState<H, V> other) {
		return new AbstractState<>(heapState.lub(other.heapState), valueState.lub(other.valueState));
	}

	@Override
	public AbstractState<H, V> widening(AbstractState<H, V> other) {
		return new AbstractState<>(heapState.widening(other.heapState), valueState.widening(other.valueState));
	}

	@Override
	public boolean lessOrEqual(AbstractState<H, V> other) {
		return heapState.lessOrEqual(other.heapState) && valueState.lessOrEqual(other.valueState);
	}

	@Override
	public AbstractState<H, V> top() {
		return new AbstractState<>(heapState.top(), valueState.top());
	}

	@Override
	public AbstractState<H, V> bottom() {
		return new AbstractState<>(heapState.bottom(), valueState.bottom());
	}

	@Override
	public boolean isTop() {
		return heapState.isTop() && valueState.isTop();
	}

	@Override
	public boolean isBottom() {
		return heapState.isBottom() && valueState.isBottom();
	}
}
