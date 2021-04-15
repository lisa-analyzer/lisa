package it.unive.lisa.analysis.lattices;

import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class ExpressionSetLattice extends SetLattice<ExpressionSetLattice, SymbolicExpression> {

	private static final ExpressionSetLattice TOP = new ExpressionSetLattice();
	private static final ExpressionSetLattice BOTTOM = new ExpressionSetLattice();

	public ExpressionSetLattice() {
		this(Collections.emptySet());
	}

	public ExpressionSetLattice(SymbolicExpression exp) {
		this(Collections.singleton(exp));
	}

	public ExpressionSetLattice(Set<SymbolicExpression> set) {
		super(set);
	}

	@Override
	public ExpressionSetLattice top() {
		return TOP;
	}

	@Override
	public ExpressionSetLattice bottom() {
		return BOTTOM;
	}

	@Override
	protected ExpressionSetLattice mk(Set<SymbolicExpression> set) {
		return new ExpressionSetLattice(set);
	}

	public int size() {
		return elements().size();
	}

	public Stream<SymbolicExpression> stream() {
		return elements().stream();
	}
}
