package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * Given an expression that evaluates to a string value and one that evaluates
 * to an integer value, a {@link BinaryExpression} using this operator yields
 * the character in the first expression at the position specified by the second
 * expression.<br>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link NumericType}<br>
 * Computed expression type: {@link CharacterType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringCharAt
		implements
		BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringCharAt INSTANCE = new StringCharAt();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringCharAt() {
	}

	@Override
	public String toString() {
		return "strcharat";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(Type::isStringType)
				|| right.stream().noneMatch(Type::isNumericType))
			return Set.of();
		return Set.of(types.getCharacterType());
	}

}
