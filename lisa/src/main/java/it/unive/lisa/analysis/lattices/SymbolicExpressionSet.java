package it.unive.lisa.analysis.lattices;

import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class SymbolicExpressionSet extends SetLattice<SymbolicExpressionSet, SymbolicExpression> {

	private static final SymbolicExpressionSet TOP = new SymbolicExpressionSet();
	private static final SymbolicExpressionSet BOTTOM = new SymbolicExpressionSet();

	public SymbolicExpressionSet() {
		this(Collections.emptySet());
	}

	public SymbolicExpressionSet(SymbolicExpression exp) {
		this(Collections.singleton(exp));
	}

	public SymbolicExpressionSet(Set<SymbolicExpression> set) {
		super(set);
	}

	@Override
	public SymbolicExpressionSet top() {
		return TOP;
	}

	@Override
	public SymbolicExpressionSet bottom() {
		return BOTTOM;
	}

	@Override
	protected SymbolicExpressionSet mk(Set<SymbolicExpression> set) {
		return new SymbolicExpressionSet(set);
	}

	public int size() {
		return elements().size();
	}

	public Stream<SymbolicExpression> stream() {
		return elements().stream();
	}
}
