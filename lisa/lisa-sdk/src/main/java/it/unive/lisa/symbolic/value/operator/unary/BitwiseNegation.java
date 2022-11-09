package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Given an expression that evaluates to a numeric value, a
 * {@link UnaryExpression} using this operator computes the bitwise negation
 * (i.e., flipping every single bit independently) of that value.<br>
 * <br>
 * Argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link NumericType} (same as argument)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BitwiseNegation implements BitwiseOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final BitwiseNegation INSTANCE = new BitwiseNegation();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected BitwiseNegation() {
	}

	@Override
	public String toString() {
		return "~";
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> argument) {
		if (argument.stream().noneMatch(Type::isNumericType))
			return Collections.emptySet();
		return argument.stream().filter(Type::isNumericType).collect(Collectors.toSet());
	}
}
