package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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

	private BitwiseNegation() {
	}

	@Override
	public String toString() {
		return "~";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> argument) {
		if (argument.noneMatch(Type::isNumericType))
			return Caches.types().mkEmptySet();
		return argument.filter(Type::isNumericType);
	}
}
