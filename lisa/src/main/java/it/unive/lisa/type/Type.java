package it.unive.lisa.type;

import java.util.Collection;

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
	public default boolean isNumericType() {
		return this instanceof NumericType;
	}

	/**
	 * Returns this type casted as a {@link NumericType}, only if
	 * {@link #isNumericType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link NumericType}, or {@code null}
	 */
	public default NumericType asNumericType() {
		return isNumericType() ? (NumericType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link BooleanType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isBooleanType() {
		return this instanceof BooleanType;
	}

	/**
	 * Returns this type casted as a {@link BooleanType}, only if
	 * {@link #isBooleanType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link BooleanType}, or {@code null}
	 */
	public default BooleanType asBooleanType() {
		return isBooleanType() ? (BooleanType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link StringType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isStringType() {
		return this instanceof StringType;
	}

	/**
	 * Returns this type casted as a {@link StringType}, only if
	 * {@link #isStringType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link StringType}, or {@code null}
	 */
	public default StringType asStringType() {
		return isStringType() ? (StringType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link Untyped}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isUntyped() {
		return this instanceof Untyped;
	}

	/**
	 * Returns this type casted as an {@link Untyped}, only if
	 * {@link #isUntyped()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link Untyped}, or {@code null}
	 */
	public default Untyped asUntyped() {
		return isUntyped() ? (Untyped) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link VoidType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isVoidType() {
		return this instanceof VoidType;
	}

	/**
	 * Returns this type casted as a {@link VoidType}, only if
	 * {@link #isVoidType()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link VoidType}, or {@code null}
	 */
	public default VoidType asVoidType() {
		return isVoidType() ? (VoidType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link PointerType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isPointerType() {
		return this instanceof PointerType;
	}

	/**
	 * Returns this type casted as a {@link PointerType}, only if
	 * {@link #isPointerType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link PointerType}, or {@code null}
	 */
	public default PointerType asPointerType() {
		return isPointerType() ? (PointerType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link ArrayType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isArrayType() {
		return this instanceof ArrayType;
	}

	/**
	 * Returns this type casted as a {@link ArrayType}, only if
	 * {@link #isArrayType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link ArrayType}, or {@code null}
	 */
	public default ArrayType asArrayType() {
		return isArrayType() ? (ArrayType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link NullType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isNullType() {
		return this instanceof NullType;
	}

	/**
	 * Returns this type casted as a {@link NullType}, only if
	 * {@link #isNullType()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link NullType}, or {@code null}
	 */
	public default NullType asNullType() {
		return isNullType() ? (NullType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link UnitType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isUnitType() {
		return this instanceof UnitType;
	}

	/**
	 * Returns this type casted as a {@link UnitType}, only if
	 * {@link #isUnitType()} yields {@code true}. Otherwise, this method returns
	 * {@code null}.
	 * 
	 * @return this type casted as {@link UnitType}, or {@code null}
	 */
	public default UnitType asUnitType() {
		return isUnitType() ? (UnitType) this : null;
	}

	/**
	 * Yields {@code true} if and only if this type is an instance of
	 * {@link TypeTokenType}.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public default boolean isTypeTokenType() {
		return this instanceof TypeTokenType;
	}

	/**
	 * Returns this type casted as a {@link TypeTokenType}, only if
	 * {@link #isTypeTokenType()} yields {@code true}. Otherwise, this method
	 * returns {@code null}.
	 * 
	 * @return this type casted as {@link TypeTokenType}, or {@code null}
	 */
	public default TypeTokenType asTypeTokenType() {
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
}
