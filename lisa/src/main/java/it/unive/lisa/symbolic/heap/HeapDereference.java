package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class HeapDereference extends HeapExpression {

	protected final SymbolicExpression toDeref;

	public HeapDereference(ExternalSet<Type> types, SymbolicExpression toDeref) {
		super(types);
		this.toDeref = toDeref;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((toDeref == null) ? 0 : toDeref.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeapDereference other = (HeapDereference) obj;
		if (toDeref == null) {
			if (other.toDeref != null)
				return false;
		} else if (!toDeref.equals(other.toDeref))
			return false;
		return true;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		T deref = toDeref.accept(visitor, params);
		return visitor.visit(this, deref, params);
	}

	public SymbolicExpression getExpression() {
		return toDeref;
	}

	@Override
	public String toString() {
		return "*(" + toDeref + ")";
	}
}
