package it.unive.lisa.symbolic.value;

import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An expression converting the type of a {@link SymbolicExpression} to the
 * given desired {@link Type}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeConversion extends ValueExpression {

	/**
	 * The type to cast operand to
	 */
	private final Type toType;

	/**
	 * The operand to cast
	 */
	private final SymbolicExpression operand;

	/**
	 * Builds the type conversion
	 * 
	 * @param toType  the type to cast {@code operand} to
	 * @param operand the expression to cast
	 */
	public TypeConversion(Type toType, SymbolicExpression operand) {
		super(operand.getTypes().filter(t -> t.canBeAssignedTo(toType)));
		this.toType = toType;
		this.operand = operand;
	}

	/**
	 * Yields the expression being casted.
	 * 
	 * @return the expression
	 */
	public SymbolicExpression getOperand() {
		return operand;
	}

	/**
	 * Yields the type that {@link #getOperand()} is being casted to.
	 * 
	 * @return the type
	 */
	public Type getToType() {
		return toType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((operand == null) ? 0 : operand.hashCode());
		result = prime * result + ((toType == null) ? 0 : toType.hashCode());
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
		TypeConversion other = (TypeConversion) obj;
		if (operand == null) {
			if (other.operand != null)
				return false;
		} else if (!operand.equals(other.operand))
			return false;
		if (toType == null) {
			if (other.toType != null)
				return false;
		} else if (!toType.equals(other.toType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "cast " + operand + "to" + toType;
	}

}
