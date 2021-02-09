package it.unive.lisa.analysis;

import it.unive.lisa.DefaultParameters;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.program.cfg.ProgramPoint;
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
@DefaultParameters({ MonolithicHeap.class, Interval.class })
public class SimpleAbstractState<H extends HeapDomain<H>, V extends ValueDomain<V>>
		implements AbstractState<SimpleAbstractState<H, V>, H, V> {

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
	public SimpleAbstractState(H heapState, V valueState) {
		this.heapState = heapState;
		this.valueState = valueState;
	}

	@Override
	public H getHeapState() {
		return heapState;
	}

	@Override
	public V getValueState() {
		return valueState;
	}

	@Override
	public SimpleAbstractState<H, V> assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		H heap = heapState.assign(id, expression, pp);
		Collection<ValueExpression> exprs = heap.getRewrittenExpressions();

		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty())
			value = value.applySubstitution(heap.getSubstitution(), pp);

		for (ValueExpression expr : exprs)
			value = value.assign(id, expr, pp);
		return new SimpleAbstractState<>(heap, value);
	}

	@Override
	public SimpleAbstractState<H, V> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		H heap = heapState.smallStepSemantics(expression, pp);
		Collection<ValueExpression> exprs = heap.getRewrittenExpressions();

		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty())
			value = value.applySubstitution(heap.getSubstitution(), pp);

		for (ValueExpression expr : exprs)
			value = value.smallStepSemantics(expr, pp);
		return new SimpleAbstractState<>(heap, value);
	}

	@Override
	public SimpleAbstractState<H, V> assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		H heap = heapState.assume(expression, pp);
		Collection<ValueExpression> exprs = heap.getRewrittenExpressions();

		V value = valueState;
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty())
			value = value.applySubstitution(heap.getSubstitution(), pp);

		for (ValueExpression expr : exprs)
			value = value.assume(expr, pp);
		return new SimpleAbstractState<>(heap, value);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		Collection<ValueExpression> rewritten = heapState.smallStepSemantics(expression, pp).getRewrittenExpressions();
		Satisfiability result = Satisfiability.BOTTOM;
		for (ValueExpression expr : rewritten)
			result = result.lub(valueState.satisfies(expr, pp));
		return heapState.satisfies(expression, pp).glb(result);
	}

	@Override
	public SimpleAbstractState<H, V> lub(SimpleAbstractState<H, V> other) throws SemanticException {
		return new SimpleAbstractState<>(heapState.lub(other.heapState), valueState.lub(other.valueState));
	}

	@Override
	public SimpleAbstractState<H, V> widening(SimpleAbstractState<H, V> other) throws SemanticException {
		return new SimpleAbstractState<>(heapState.widening(other.heapState), valueState.widening(other.valueState));
	}

	@Override
	public boolean lessOrEqual(SimpleAbstractState<H, V> other) throws SemanticException {
		return heapState.lessOrEqual(other.heapState) && valueState.lessOrEqual(other.valueState);
	}

	@Override
	public SimpleAbstractState<H, V> top() {
		return new SimpleAbstractState<>(heapState.top(), valueState.top());
	}

	@Override
	public SimpleAbstractState<H, V> bottom() {
		return new SimpleAbstractState<>(heapState.bottom(), valueState.bottom());
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
	public SimpleAbstractState<H, V> forgetIdentifier(Identifier id) throws SemanticException {
		return new SimpleAbstractState<>(heapState.forgetIdentifier(id), valueState.forgetIdentifier(id));
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
		SimpleAbstractState<?, ?> other = (SimpleAbstractState<?, ?>) obj;
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
