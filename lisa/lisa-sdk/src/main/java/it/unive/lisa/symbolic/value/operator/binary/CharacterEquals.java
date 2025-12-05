package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * Given two expressions that evaluate to character values, a
 * {@link BinaryExpression} using this operator yields a boolean value that is
 * {@code true} if both characters are equal, {@code false} otherwise.<br>
 * <br>
 * First argument expression type: {@link CharacterType} Second argument
 * expression type: {@link CharacterType} Computed expression type:
 * {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CharacterEquals
		implements
		BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final CharacterEquals INSTANCE = new CharacterEquals();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected CharacterEquals() {
	}

	@Override
	public String toString() {
		return "chreq";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (left.stream().noneMatch(Type::isCharacterType)
				|| right.stream().noneMatch(Type::isCharacterType))
			return Set.of();
		return Set.of(types.getBooleanType());
	}

}
