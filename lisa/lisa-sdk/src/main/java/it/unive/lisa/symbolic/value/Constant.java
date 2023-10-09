package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A constant value.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Constant extends ValueExpression {

	/**
	 * The constant
	 */
	private final Object value;

	/**
	 * Builds the constant.
	 * 
	 * @param type     the type of the constant
	 * @param value    the constant value
	 * @param location the code location of the statement that has generated
	 *                     this constant
	 */
	public Constant(
			Type type,
			Object value,
			CodeLocation location) {
		super(type, location);
		this.value = value;
	}

	/**
	 * Yields the constant value.
	 * 
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Constant other = (Constant) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token) {
		return this;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token)
			throws SemanticException {
		return this;
	}

	@Override
	public String toString() {
		return value instanceof String ? "\"" + value + "\"" : value.toString();
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		return visitor.visit(this, params);
	}

	@Override
	public boolean mightNeedRewriting() {
		Type t = getStaticType();
		return !t.isValueType() || t.isUntyped();
	}
}
