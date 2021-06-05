package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class MemoryPointer extends Identifier {

	private final HeapLocation loc;
	
	public MemoryPointer(ExternalSet<Type> types, HeapLocation loc) {
		this(types, loc, new Annotations());
	}
	
	public MemoryPointer(ExternalSet<Type> types, HeapLocation loc, Annotations annotations) {
		// A pointer identifier is always a strong identifier
		super(types, loc.getName(), false, annotations);
		this.loc = loc;
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return new OutOfScopeIdentifier(this, token);
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return null;
	}

	public HeapLocation getLocation() {
		return loc;
	}
	
	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}

	@Override
	public String toString() {
		return "&" + getName();
	}
}
