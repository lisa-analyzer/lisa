package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * Given an expression that evaluates to a string and one that evaluates to a
 * character, {@link BinaryExpression} using this operator computes the index
 * <i>of the first occurrence</i> of the second argument inside the string of
 * the first argument, producing {@code -1} if no occurrence can be found.<br>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link CharacterType}<br>
 * Computed expression type: {@link NumericType} (integral)
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringIndexOfChar
		implements
		BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringIndexOfChar INSTANCE = new StringIndexOfChar();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringIndexOfChar() {
	}

	@Override
	public String toString() {
		return "strindexofchar";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(Type::isStringType)
				|| right.stream().noneMatch(Type::isCharacterType))
			return Set.of();
		return Set.of(types.getIntegerType());
	}

}
