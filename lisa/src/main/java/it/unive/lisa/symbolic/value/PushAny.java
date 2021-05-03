package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An expression converting that push any possible value on the stack. This is
 * useful to represent top values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class PushAny extends ValueExpression {

	/**
	 * Builds the push any.
	 * 
	 * @param types the runtime types of this expression
	 */
	public PushAny(ExternalSet<Type> types) {
		super(types);
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
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
