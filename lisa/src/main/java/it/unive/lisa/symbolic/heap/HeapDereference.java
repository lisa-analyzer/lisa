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
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
	
	public SymbolicExpression getExpression() {
		return toDeref;
	}

	@Override
	public String toString() {
		return "*(" + toDeref + ")";
	}
}
