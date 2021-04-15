package it.unive.lisa.analysis.lattices;

import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class ValueExpressionSet extends SetLattice<ValueExpressionSet, ValueExpression> {

	private static final ValueExpressionSet TOP = new ValueExpressionSet();
	private static final ValueExpressionSet BOTTOM = new ValueExpressionSet();

	public ValueExpressionSet() {
		this(Collections.emptySet());
	}

	public ValueExpressionSet(ValueExpression exp) {
		this(Collections.singleton(exp));
	}

	public ValueExpressionSet(Set<ValueExpression> set) {
		super(set);
	}

	@Override
	public ValueExpressionSet top() {
		return TOP;
	}

	@Override
	public ValueExpressionSet bottom() {
		return BOTTOM;
	}

	@Override
	protected ValueExpressionSet mk(Set<ValueExpression> set) {
		return new ValueExpressionSet(set);
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
