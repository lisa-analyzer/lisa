package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collection;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout and a value state modeling values of program variables and
 * memory locations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the type of {@link HeapDomain} embedded in this state
 * @param <V> the type of {@link ValueDomain} embedded in this state
 */
public class AbstractState<H extends HeapDomain<H>, V extends ValueDomain<V>>
		implements Lattice<AbstractState<H, V>>, SemanticDomain<AbstractState<H, V>, SymbolicExpression, Identifier> {

	/**
	 * The domain containing information regarding heap structures
	 */
	private final H heapState;

	/**
	 * The domain containing information regarding values of program variables
	 * and concretized memory locations
	 */
	private final V valueState;

	/**
	 * Builds a new abstract state.
	 * 
	 * @param heapState  the domain containing information regarding heap
	 *                       structures
	 * @param valueState the domain containing information regarding values of
	 *                       program variables and concretized memory locations
	 */
	public AbstractState(H heapState, V valueState) {
		this.heapState = heapState;
		this.valueState = valueState;
	}

	/**
	 * Yields the instance of {@link HeapDomain} that contains the information
	 * on heap structures contained in this abstract state.
	 * 
	 * @return the heap domain
	 */
	public H getHeapState() {
		return heapState;
	}

	/**
	 * Yields the instance of {@link ValueDomain} that contains the information
	 * on values of program variables and concretized memory locations.
	 * 
	 * @return the value domain
	 */
	public V getValueState() {
		return valueState;
	}

	@Override
	public AbstractState<H, V> assign(Identifier id, SymbolicExpression expression) throws SemanticException {
		H heap = heapState.assign(id, expression);
		Collection<ValueExpression> exprs = heap.getRewrittenExpressions();

		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty())
			value = value.applySubstitution(heap.getSubstitution());

		for (ValueExpression expr : exprs)
			value = value.assign(id, expr);
		return new AbstractState<>(heap, value);
	}

	@Override
	public AbstractState<H, V> smallStepSemantics(SymbolicExpression expression) throws SemanticException {
		H heap = heapState.smallStepSemantics(expression);
		Collection<ValueExpression> exprs = heap.getRewrittenExpressions();

		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty())
			value = value.applySubstitution(heap.getSubstitution());

		for (ValueExpression expr : exprs)
			value = value.smallStepSemantics(expr);
		return new AbstractState<>(heap, value);
	}

	@Override
	public AbstractState<H, V> assume(SymbolicExpression expression) throws SemanticException {
		H heap = heapState.assume(expression);
		Collection<ValueExpression> exprs = heap.getRewrittenExpressions();

		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty())
			value = value.applySubstitution(heap.getSubstitution());

		for (ValueExpression expr : exprs)
			value = value.assume(expr);
		return new AbstractState<>(heap, value);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression) throws SemanticException {
		Collection<ValueExpression> rewritten = heapState.smallStepSemantics(expression).getRewrittenExpressions();
		Satisfiability result = Satisfiability.BOTTOM;
		for (ValueExpression expr : rewritten)
			result = result.lub(valueState.satisfies(expr));
		return heapState.satisfies(expression).glb(result);
	}

	@Override
	public AbstractState<H, V> lub(AbstractState<H, V> other) throws SemanticException {
		return new AbstractState<>(heapState.lub(other.heapState), valueState.lub(other.valueState));
	}

	@Override
	public AbstractState<H, V> widening(AbstractState<H, V> other) throws SemanticException {
		return new AbstractState<>(heapState.widening(other.heapState), valueState.widening(other.valueState));
	}

	@Override
	public boolean lessOrEqual(AbstractState<H, V> other) throws SemanticException {
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

	@Override
	public AbstractState<H, V> forgetIdentifier(Identifier id) throws SemanticException {
		return new AbstractState<>(heapState.forgetIdentifier(id), valueState.forgetIdentifier(id));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((heapState == null) ? 0 : heapState.hashCode());
		result = prime * result + ((valueState == null) ? 0 : valueState.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractState<?, ?> other = (AbstractState<?, ?>) obj;
		if (heapState == null) {
			if (other.heapState != null)
				return false;
		} else if (!heapState.equals(other.heapState))
			return false;
		if (valueState == null) {
			if (other.valueState != null)
				return false;
		} else if (!valueState.equals(other.valueState))
			return false;
		return true;
	}

	@Override
	public String representation() {
		return "heap [[ " + heapState.representation() + " ]]\nvalue [[ " + valueState.representation() + " ]]";
	}

	@Override
	public String toString() {
		return representation();
	}
}
