package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.TypeOperator;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import java.util.Collections;
import java.util.Set;

/**
 * Given two expressions, with the second one evaluating to a type token, a
 * {@link BinaryExpression} using this operator checks if the runtime types of
 * the value the first argument evaluates to are subtypes of the ones contained
 * in the token (that is, if the value is an instance of one of them, see
 * {@link it.unive.lisa.type.Type#allInstances(it.unive.lisa.type.TypeSystem)}).
 * This is not whether the value can be cast to one of them (see
 * {@link TypeCast}): for instance, an integer value is not an instance of a
 * floating point type, even though it can be cast to it.<br>
 * <br>
 * A token with several types denotes all of them: the value is an instance of
 * the token if it is an instance of any of its types (as for a tuple of classes
 * passed to Python's {@code isinstance}). Instead, when the second argument
 * might evaluate to several tokens (e.g., its runtime types contain more than
 * one {@link TypeTokenType}), these are alternatives: each execution evaluates
 * it to one of them, and the check surely holds only if it holds for every
 * token.<br>
 * <br>
 * First argument expression type: any {@link Type}<br>
 * Second argument expression type: {@link TypeTokenType}<br>
 * Computed expression type: {@link BooleanType}
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeCheck
		implements
		TypeOperator,
		BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final TypeCheck INSTANCE = new TypeCheck();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected TypeCheck() {
	}

	@Override
	public String toString() {
		return "is";
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		if (right.stream().noneMatch(Type::isTypeTokenType))
			return Collections.emptySet();
		return Collections.singleton(types.getBooleanType());
	}

}
