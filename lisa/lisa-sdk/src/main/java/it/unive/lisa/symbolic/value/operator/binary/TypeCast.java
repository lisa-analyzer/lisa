package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.TypeOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * Given two expressions, with the second one evaluating to a type token, a
 * {@link BinaryExpression} using this operator casts the type of the first
 * argument to the type of the second argument. The resulting value is exactly
 * the first argument, but with its runtime types <i>filtered</i> to be
 * instances of the right-hand side type. Indeed, this operation on types is a
 * narrowing operator, that is, the destination type is smaller than the source
 * type.<br>
 * This operator resembles the Java cast operation between reference types: when
 * an object is cast to another type, its runtime types do not change, but the
 * cast fails of the runtime type of the object is not a subtype of the desired
 * type.<br>
 * <br>
 * First argument expression type: any {@link Type}<br>
 * Second argument expression type: {@link TypeTokenType}<br>
 * Computed expression type: first argument {@link Type}s filtered
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeCast implements TypeOperator, BinaryOperator {

	/**
	 * The singleton instance of this class.
	 */
	public static final TypeCast INSTANCE = new TypeCast();

	private TypeCast() {
	}

	@Override
	public String toString() {
		return "cast-as";
	}

	@Override
	public ExternalSet<Type> typeInference(ExternalSet<Type> left, ExternalSet<Type> right) {
		if (right.noneMatch(Type::isTypeTokenType))
			return Caches.types().mkEmptySet();
		ExternalSet<Type> set = Type.cast(left, right);
		if (set.isEmpty())
			return Caches.types().mkEmptySet();
		return set;
	}
}
