package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.TypeOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import java.util.Collections;
import java.util.Set;

/**
 * Given two expressions, with the second one evaluating to a type token, a
 * {@link BinaryExpression} using this operator converts the type of the first
 * argument to the type of the second argument. The resulting value is exactly
 * the first argument, but with its runtime types <i>changed</i> to be instances
 * of the right-hand side type. Indeed, this operation on types is a widening
 * operator, that is, the destination type is greater than the source type.<br>
 * This operator resembles the Java cast operation between primitive types:
 * when, for instance, an int is cast to a float, its runtime types does change,
 * possibly with loss of information during the conversion.<br>
 * <br>
 * First argument expression type: any {@link Type}<br>
 * Second argument expression type: {@link TypeTokenType}<br>
 * Computed expression type: second argument inner {@link Type}s
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeConv implements TypeOperator, BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final TypeConv INSTANCE = new TypeConv();

	/**
	 * Builds the operator. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected TypeConv() {
	}

	@Override
	public String toString() {
		return "conv-as";
	}

	@Override
	public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
		if (right.stream().noneMatch(Type::isTypeTokenType))
			return Collections.emptySet();
		Set<Type> set = types.convert(left, right);
		if (set.isEmpty())
			return Collections.emptySet();
		return set;
	}
}
