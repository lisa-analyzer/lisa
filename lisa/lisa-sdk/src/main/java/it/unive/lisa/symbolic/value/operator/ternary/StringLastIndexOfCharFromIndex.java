package it.unive.lisa.symbolic.value.operator.ternary;

import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * Given an expression that evaluates to a string, one that evaluates to a
 * character, and one that evaluates to an integer, a {@link TernaryExpression}
 * using this operator computes the index <i>of the last occurrence</i> of the
 * second argument inside the string of the first argument, searching backwards
 * from the position of the third argument and producing {@code -1} if no
 * occurrence can be found.<br>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link CharacterType}<br>
 * Third argument expression type: {@link NumericType} (integral)<br>
 * Computed expression type: {@link NumericType} (integral)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringLastIndexOfCharFromIndex
		implements
		TernaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringLastIndexOfCharFromIndex INSTANCE = new StringLastIndexOfCharFromIndex();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringLastIndexOfCharFromIndex() {
	}

	@Override
	public String toString() {
		return "strlastindexofcharfrom";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> middle,
			Set<Type> right) {
		if (left.stream().noneMatch(Type::isStringType)
				|| middle.stream().noneMatch(Type::isCharacterType)
				|| right.stream().noneMatch(Type::isNumericType))
			return Set.of();
		return Set.of(types.getIntegerType());
	}

}
