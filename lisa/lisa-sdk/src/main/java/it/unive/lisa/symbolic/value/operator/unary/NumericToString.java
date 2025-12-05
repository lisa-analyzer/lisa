package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Given an expression that evaluates to a numeric value, a
 * {@link UnaryExpression} using this operator converts the value to a
 * string.<br>
 * <br>
 * Argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link StringType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NumericToString
		implements
		ArithmeticOperator,
		UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final NumericToString INSTANCE = new NumericToString();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected NumericToString() {
	}

	@Override
	public String toString() {
		return "toString";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(Type::isNumericType))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}

}
