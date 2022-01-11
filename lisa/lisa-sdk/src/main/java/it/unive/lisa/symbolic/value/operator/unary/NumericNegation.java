package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Given an expression that evaluates to a numeric value, a
 * {@link UnaryExpression} using this operator computes the arithmetic negation
 * (i.e., multiplication by {@code -1}) of that value.<br>
 * <br>
 * Argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link NumericType} (same of the argument, but
 * signed)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NumericNegation implements ArithmeticOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final NumericNegation INSTANCE = new NumericNegation();

	private NumericNegation() {
	}

	@Override
	public String toString() {
		return "-";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> argument) {
		if (argument.noneMatch(Type::isNumericType))
			return Caches.types().mkEmptySet();
		return argument.filter(Type::isNumericType);
	}
}
