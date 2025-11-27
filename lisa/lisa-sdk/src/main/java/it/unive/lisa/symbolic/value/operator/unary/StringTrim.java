package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Given an expression that evaluates to a string value, a
 * {@link UnaryExpression} using this operator removes leading and trailing
 * whitespace characters from the string.<br>
 * <br>
 * Argument expression type: {@link StringType}<br>
 * Computed expression type: {@link StringType} (integral)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringTrim
		implements
		StringOperator,
		UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringTrim INSTANCE = new StringTrim();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringTrim() {
	}

	@Override
	public String toString() {
		return "strtrim";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(Type::isStringType))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}

}
