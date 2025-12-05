package it.unive.lisa.symbolic.value.operator.ternary;

import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * Given two expressions that both evaluate to string values, and a third one
 * evaluating to an integer value, a {@link TernaryExpression} using this
 * operator computes the starting index <i>of the last occurrence</i> of the
 * string from the second argument inside the one of the first argument,
 * searching backwards from the position defined by the third argument and
 * producing {@code -1} if no occurrence can be found.<br>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link StringType}<br>
 * Third argument expression type: {@link NumericType} (integral)<br>
 * Computed expression type: {@link NumericType} (integral)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringLastIndexOfFromIndex
		implements
		TernaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringLastIndexOfFromIndex INSTANCE = new StringLastIndexOfFromIndex();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringLastIndexOfFromIndex() {
	}

	@Override
	public String toString() {
		return "strlastindexoffrom";
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
		return Set.of(types.getIntegerType());
	}

}
