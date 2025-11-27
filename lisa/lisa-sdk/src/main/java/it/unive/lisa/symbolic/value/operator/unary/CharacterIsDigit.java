package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * Given an expression that evaluates to a character value, a
 * {@link UnaryExpression} using this operator yields {@code true} if the
 * character is a digit (as defined by the Unicode standard), {@code false}
 * otherwise.<br>
 * <br>
 * Argument expression type: {@link CharacterType}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CharacterIsDigit
		implements
		UnaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final CharacterIsDigit INSTANCE = new CharacterIsDigit();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected CharacterIsDigit() {
	}

	@Override
	public String toString() {
		return "isDigit";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		if (argument.stream().noneMatch(Type::isCharacterType))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}

}
