package it.unive.lisa.analysis.lattices;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Skip;
import java.util.Collections;
import java.util.Set;

public class ExpressionSet<T extends SymbolicExpression> extends SetLattice<ExpressionSet<T>, T> {

	public ExpressionSet() {
		this(Collections.emptySet());
	}

	public ExpressionSet(T exp) {
		this(Collections.singleton(exp));
	}

	public ExpressionSet(Set<T> set) {
		super(set);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ExpressionSet<T> top() {
		return new ExpressionSet<T>((T) new Skip());
	}

	@Override
	public ExpressionSet<T> bottom() {
		return new ExpressionSet<T>();
	}

	@Override
	protected ExpressionSet<T> mk(Set<T> set) {
		return new ExpressionSet<T>(set);
	}
}
