package it.unive.lisa.symbolic.value.operator.ternary;

import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.operator.StringOperator;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Given three expressions that all evaluate to string values, a
 * {@link TernaryExpression} using this operator computes a new string
 * corresponding to the first argument's string where all occurrences of the
 * second argument's string have been replaced with the third argument's
 * string.<br>
 * Note that:
 * <ul>
 * <li>if the first argument's string is empty, the empty string is
 * returned</li>
 * <li>if the second argument's string is empty, the third argument's string is
 * added in any position of the first argument's string</li>
 * <li>if the third argument's string is empty, occurrences of the second
 * argument's string are simply removed from the first argument's string</li>
 * </ul>
 * <br>
 * First argument expression type: {@link StringType}<br>
 * Second argument expression type: {@link StringType}<br>
 * Third argument expression type: {@link StringType}<br>
 * Computed expression type: {@link StringType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringReplace implements StringOperator, TernaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringReplace INSTANCE = new StringReplace();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringReplace() {
	}

	@Override
	public String toString() {
		return "strreplace";
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> middle, Set<Type> right) {
		if (left.stream().noneMatch(Type::isStringType)
				|| middle.stream().noneMatch(Type::isStringType)
				|| right.stream().noneMatch(Type::isStringType))
			return Collections.emptySet();
		return Collections.singleton(types.getStringType());
	}
}
