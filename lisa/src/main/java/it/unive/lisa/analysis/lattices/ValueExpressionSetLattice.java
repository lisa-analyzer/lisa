package it.unive.lisa.analysis.lattices;

import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class ValueExpressionSetLattice extends SetLattice<ValueExpressionSetLattice, ValueExpression> {

	private static final ValueExpressionSetLattice TOP = new ValueExpressionSetLattice();
	private static final ValueExpressionSetLattice BOTTOM = new ValueExpressionSetLattice();

	public ValueExpressionSetLattice() {
		this(Collections.emptySet());
	}

	public ValueExpressionSetLattice(ValueExpression exp) {
		this(Collections.singleton(exp));
	}

	public ValueExpressionSetLattice(Set<ValueExpression> set) {
		super(set);
	}

	@Override
	public ValueExpressionSetLattice top() {
		return TOP;
	}

	@Override
	public ValueExpressionSetLattice bottom() {
		return BOTTOM;
	}

	@Override
	protected ValueExpressionSetLattice mk(Set<ValueExpression> set) {
		return new ValueExpressionSetLattice(set);
	}

	public int size() {
		return elements().size();
	}

	public Stream<ValueExpression> stream() {
		return elements().stream();
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

}
