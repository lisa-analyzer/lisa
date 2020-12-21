package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

public abstract class CartesianProduct<T1 extends SemanticDomain<T1, E, I>, T2 extends SemanticDomain<T2, E, I>, E extends SymbolicExpression, I extends Identifier> {

	protected T1 left;
	protected T2 right;

	protected CartesianProduct(T1 left, T2 right) {
		this.left = left;
		this.right = right;
	}
}
