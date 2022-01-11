package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.Int32;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Given an expression that evaluates to a string value, a
 * {@link UnaryExpression} using this operator computes an integral value, from
 * {@code 0} upwards, representing the length of that value.<br>
 * <br>
 * Argument expression type: {@link StringType}<br>
 * Computed expression type: {@link NumericType} (integral)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringLength implements StringOperator, UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringLength INSTANCE = new StringLength();

	private StringLength() {
	}

	@Override
	public String toString() {
		return "strlen";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> argument) {
		if (argument.noneMatch(Type::isStringType))
			return Caches.types().mkEmptySet();
		return Caches.types().mkSingletonSet(Int32.INSTANCE);
	}
}
