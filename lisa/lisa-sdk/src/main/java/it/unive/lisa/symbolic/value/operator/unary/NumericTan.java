package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Given an expression that evaluates to a numeric value, a
 * {@link UnaryExpression} using this operator computes the tanget of that
 * value.<br>
 * <br>
 * Argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link NumericType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NumericTan
		implements
		ArithmeticOperator,
		UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final NumericTan INSTANCE = new NumericTan();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected NumericTan() {
	}

	@Override
	public String toString() {
		return "tan";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(Type::isNumericType))
			return Collections.emptySet();
		return argument.stream().filter(Type::isNumericType).collect(Collectors.toSet());
	}

}
