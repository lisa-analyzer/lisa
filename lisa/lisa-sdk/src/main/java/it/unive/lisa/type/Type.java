package it.unive.lisa.type;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Type interface. Any instance of a concrete type, instance of Type, should be
 * unique and implemented following the singleton design pattern (see for
 * instance {@link Untyped} class).
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public interface Type {

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link NumericType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isNumericType() {
		return this instanceof NumericType;
	}

	/**
	 * Returns this type casted as a {@link NumericType}, only if
	 * {@link #isNumericType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link NumericType}, or {@code null}
	 */
	default NumericType asNumericType() {
		return isNumericType() ? (NumericType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link BooleanType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isBooleanType() {
		return this instanceof BooleanType;
	}

	/**
	 * Returns this type casted as a {@link BooleanType}, only if
	 * {@link #isBooleanType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link BooleanType}, or {@code null}
	 */
	default BooleanType asBooleanType() {
		return isBooleanType() ? (BooleanType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link StringType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isStringType() {
		return this instanceof StringType;
	}

	/**
	 * Returns this type casted as a {@link StringType}, only if
	 * {@link #isStringType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link StringType}, or {@code null}
	 */
	default StringType asStringType() {
		return isStringType() ? (StringType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link Untyped}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isUntyped() {
		return this instanceof Untyped;
	}

	/**
	 * Returns this type casted as an {@link Untyped}, only if
	 * {@link #isUntyped()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link Untyped}, or {@code null}
	 */
	default Untyped asUntyped() {
		return isUntyped() ? (Untyped) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link VoidType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isVoidType() {
		return this instanceof VoidType;
	}

	/**
	 * Returns this type casted as a {@link VoidType}, only if
	 * {@link #isVoidType()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link VoidType}, or {@code null}
	 */
	default VoidType asVoidType() {
		return isVoidType() ? (VoidType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link PointerType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isPointerType() {
		return this instanceof PointerType;
	}

	/**
	 * Returns this type casted as a {@link PointerType}, only if
	 * {@link #isPointerType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link PointerType}, or {@code null}
	 */
	default PointerType asPointerType() {
		return isPointerType() ? (PointerType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link InMemoryType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isInMemoryType() {
		return this instanceof InMemoryType;
	}

	/**
	 * Returns this type casted as a {@link InMemoryType}, only if
	 * {@link #isInMemoryType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link InMemoryType}, or {@code null}
	 */
	default InMemoryType asInMemoryType() {
		return isInMemoryType() ? (InMemoryType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link ArrayType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isArrayType() {
		return this instanceof ArrayType;
	}

	/**
	 * Returns this type casted as a {@link ArrayType}, only if
	 * {@link #isArrayType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link ArrayType}, or {@code null}
	 */
	default ArrayType asArrayType() {
		return isArrayType() ? (ArrayType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link NullType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isNullType() {
		return this instanceof NullType;
	}

	/**
	 * Returns this type casted as a {@link NullType}, only if
	 * {@link #isNullType()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link NullType}, or {@code null}
	 */
	default NullType asNullType() {
		return isNullType() ? (NullType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link UnitType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isUnitType() {
		return this instanceof UnitType;
	}

	/**
	 * Returns this type casted as a {@link UnitType}, only if
	 * {@link #isUnitType()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link UnitType}, or {@code null}
	 */
	default UnitType asUnitType() {
		return isUnitType() ? (UnitType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link TypeTokenType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isTypeTokenType() {
		return this instanceof TypeTokenType;
	}

	/**
	 * Returns this type casted as a {@link TypeTokenType}, only if
	 * {@link #isTypeTokenType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link TypeTokenType}, or {@code null}
	 */
	default TypeTokenType asTypeTokenType() {
		return isTypeTokenType() ? (TypeTokenType) this : null;
	}

	/**
	 * Determines if the type represented by this {@link Type} object is either
	 * the same as, or is a subtype of, the type represented by {@code other}.
	 * It returns {@code true} if so, and returns {@code false} otherwise.
	 * 
	 * @param other the other type
	 * 
	 * @return {@code true} if that condition holds
	 */
	boolean canBeAssignedTo(Type other);

	/**
	 * Yields the most specific common supertype between this {@link Type} and
	 * the given one. If no common supertype exists, this method returns
	 * {@link Untyped#INSTANCE}.
	 * 
	 * @param other the other type
	 * 
	 * @return the most specific common supertype between {@code this} and
	 *             {@code other}
	 */
	Type commonSupertype(Type other);

	/**
	 * Yields all possible instances of this type, including itself.
	 * 
	 * @return the possible instances
	 */
	Collection<Type> allInstances();

	/**
	 * Simulates a cast operation, where an expression with possible runtime
	 * types {@code types} is being cast to one of the possible type tokens in
	 * {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code types}, keeping only the ones that can be assigned to one of the
	 * types represented by one of the tokens in {@code tokens}.<br>
	 * <br>
	 * Invoking this method is equivalent to invoking
	 * {@link #cast(ExternalSet, ExternalSet, AtomicBoolean)} passing
	 * {@code null} as third parameter.
	 * 
	 * @param types  the types of the expression being casted
	 * @param tokens the tokens representing the operand of the cast
	 * 
	 * @return the set of possible types after the cast
	 */
	public static ExternalSet<Type> cast(ExternalSet<Type> types, ExternalSet<Type> tokens) {
		return cast(types, tokens, null);
	}

	/**
	 * Simulates a cast operation, where an expression with possible runtime
	 * types {@code types} is being cast to one of the possible type tokens in
	 * {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code types}, keeping only the ones that can be assigned to one of the
	 * types represented by one of the tokens in {@code tokens}.
	 * 
	 * @param types     the types of the expression being casted
	 * @param tokens    the tokens representing the operand of the cast
	 * @param mightFail a reference to the boolean to set if this cast might
	 *                      fail (e.g., casting an [int, string] to an int will
	 *                      yield a set containing int and will set the boolean
	 *                      to true)
	 * 
	 * @return the set of possible types after the cast
	 */
	public static ExternalSet<Type> cast(ExternalSet<Type> types, ExternalSet<Type> tokens, AtomicBoolean mightFail) {
		if (mightFail != null)
			mightFail.set(false);

		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type token : tokens.filter(Type::isTypeTokenType).multiTransform(t -> t.asTypeTokenType().getTypes()))
			for (Type t : types)
				if (t.canBeAssignedTo(token))
					result.add(t);
				else if (mightFail != null)
					mightFail.set(true);

		return result;
	}

	/**
	 * Simulates a conversion operation, where an expression with possible
	 * runtime types {@code types} is being converted to one of the possible
	 * type tokens in {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code tokens}, keeping only the ones such that there exists at least one
	 * type in {@code types} that can be assigned to it.
	 * 
	 * @param types  the types of the expression being converted
	 * @param tokens the tokens representing the operand of the type conversion
	 * 
	 * @return the set of possible types after the type conversion
	 */
	public static ExternalSet<Type> convert(ExternalSet<Type> types, ExternalSet<Type> tokens) {
		ExternalSet<Type> result = Caches.types().mkEmptySet();
		for (Type token : tokens.filter(Type::isTypeTokenType).multiTransform(t -> t.asTypeTokenType().getTypes()))
			for (Type t : types)
				if (t.canBeAssignedTo(token))
					result.add(token);

		return result;
	}
}
