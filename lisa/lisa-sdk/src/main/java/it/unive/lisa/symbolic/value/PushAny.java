package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * An expression that pushes any possible value on the stack. This is useful to
 * represent top values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class PushAny extends ValueExpression {

	/**
	 * Builds the push any.
	 * 
	 * @param staticType the static type of this expression
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public PushAny(Type staticType, CodeLocation location) {
		super(staticType, location);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PUSHANY";
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return this;
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return this;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
