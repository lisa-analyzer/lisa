package it.unive.lisa.type;

import java.util.Collection;
import java.util.Set;

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
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link ReferenceType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	default boolean isReferenceType() {
		return this instanceof ReferenceType;
	}

	/**
	 * Returns this type casted as a {@link ReferenceType}, only if
	 * {@link #isReferenceType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link ReferenceType}, or {@code null}
	 */
	default ReferenceType asReferenceType() {
		return isReferenceType() ? (ReferenceType) this : null;
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
	boolean canBeAssignedTo(
			Type other);

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
	Type commonSupertype(
			Type other);

	/**
	 * Yields all possible instances of this type, including itself.
	 * 
	 * @param types the type system that can be used to look for subtypes of
	 *                  this type
	 * 
	 * @return the possible instances
	 */
	Set<Type> allInstances(
			TypeSystem types);

	/**
	 * Yields the most specific common supertype of the given collection of
	 * types by successive invocations of {@link #commonSupertype(Type)} between
	 * its elements.
	 * 
	 * @param types    the types
	 * @param fallback the type to return if the collection is empty
	 * 
	 * @return the most specific common supertype, or {@code fallback}
	 */
	public static Type commonSupertype(
			Collection<Type> types,
			Type fallback) {
		if (types == null || types.isEmpty())
			return fallback;
		Type result = null;
		for (Type t : types)
			if (result == null)
				result = t;
			else
				result = result.commonSupertype(t);
		return result;
	}
}
