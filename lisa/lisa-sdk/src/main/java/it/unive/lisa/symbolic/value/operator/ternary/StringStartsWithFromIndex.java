package it.unive.lisa.symbolic.value.operator.ternary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * Given two expressions that both evaluate to string values, a
 * {@link BinaryExpression} using this operator checks if the string from the
 * first argument is prefixed by the one of the second argument.<br>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link StringType}<br>
 * Third argument expression type: {@link NumericType} (integral)<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringStartsWithFromIndex
		implements
		TernaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringStartsWithFromIndex INSTANCE = new StringStartsWithFromIndex();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringStartsWithFromIndex() {
	}

	@Override
	public String toString() {
		return "strstartsfrom";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> middle,
			Set<Type> right) {
		if (left.stream().noneMatch(Type::isStringType)
				|| middle.stream().noneMatch(Type::isStringType)
				|| right.stream().noneMatch(Type::isNumericType))
			return Set.of();
		return Set.of(types.getBooleanType());
	}

}
