package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.TypeOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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

	private TypeConv() {
	}

	@Override
	public String toString() {
		return "conv-as";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> right) {
		if (right.noneMatch(Type::isTypeTokenType))
			return Caches.types().mkEmptySet();
		ExternalSet<Type> set = Type.convert(left, right);
		if (set.isEmpty())
			return Caches.types().mkEmptySet();
		return set;
	}
}
