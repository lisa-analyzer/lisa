package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class PointerIdentifier extends Identifier {

	private final HeapLocation loc;
	
	public PointerIdentifier(ExternalSet<Type> types, HeapLocation loc) {
		this(types, loc, new Annotations());
	}
	
	public PointerIdentifier(ExternalSet<Type> types, HeapLocation loc, Annotations annotations) {
		// A pointer identifier is always a strong identifier
		super(types, loc.getName(), false, annotations);
		this.loc = loc;
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) throws SemanticException {
		return this;
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return this;
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
