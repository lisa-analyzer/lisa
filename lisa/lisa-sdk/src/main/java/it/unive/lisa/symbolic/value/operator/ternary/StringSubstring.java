package it.unive.lisa.symbolic.value.operator.ternary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.common.StringType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Given three expressions, with the first one evaluating to a string value and
 * the second and third one evaluating to integral numerical values, a
 * {@link TernaryExpression} using this operator computes a new string
 * corresponding to the portion of first argument's string starting at the
 * second argument's number position (inclusive) and ending at the third
 * argument's number position (exclusive).<br>
 * Note that:
 * <ul>
 * <li>both second and third argument's numbers must be non-negative and less
 * than the length of the first argument's string, with the second one's less or
 * equal than the third one's</li>
 * <li>if the second and third argument's numbers are equal, the empty string is
 * returned</li>
 * </ul>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link NumericType}<br>
 * Third argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link StringType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringSubstring implements StringOperator, TernaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringSubstring INSTANCE = new StringSubstring();

	private StringSubstring() {
	}

	@Override
	public String toString() {
		return "substr";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> middle, ExternalSet<Type> right) {
		if (left.noneMatch(Type::isStringType)
				|| middle.noneMatch(Type::isNumericType)
				|| middle.filter(Type::isNumericType).noneMatch(t -> t.asNumericType().isIntegral())
				|| right.noneMatch(Type::isNumericType)
				|| right.filter(Type::isNumericType).noneMatch(t -> t.asNumericType().isIntegral()))
			return Caches.types().mkEmptySet();
		return Caches.types().mkSingletonSet(StringType.INSTANCE);
	}
}
