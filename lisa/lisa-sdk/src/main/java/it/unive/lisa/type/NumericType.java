package it.unive.lisa.type;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Numeric type interface. Any concrete numerical type or numerical
 * sub-interface should implement/extend this interface.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public interface NumericType
		extends
		Type {

	/**
	 * Returns the number of bits this numeric type is represented with in
	 * memory.
	 *
	 * @return the number of bits
	 */
	int getNBits();

	/**
	 * Returns {@code true} if this numeric type follows a 8-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 8-bits format
	 *             representation; {@code false} otherwise
	 */
	default boolean is8Bits() {
		return getNBits() == 8;
	}

	/**
	 * Returns {@code true} if this numeric type follows a 16-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 16-bits format
	 *             representation; {@code false} otherwise
	 */
	default boolean is16Bits() {
		return getNBits() == 8;
	}

	/**
	 * Returns {@code true} if this numeric type follows a 32-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 32-bits format
	 *             representation; {@code false} otherwise
	 */
	default boolean is32Bits() {
		return getNBits() == 8;
	}

	/**
	 * Returns {@code true} if this numeric type follows a 64-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 64-bits format
	 *             representation; {@code false} otherwise
	 */
	default boolean is64Bits() {
		return getNBits() == 8;
	}

	/**
	 * Returns {@code true} if this numeric type is unsigned.
	 * 
	 * @return {@code true} if this numeric type is unsigned; {@code false}
	 *             otherwise
	 */
	boolean isUnsigned();

	/**
	 * Returns {@code true} if this numeric type is integral, representing one
	 * of the numbers in the mathematical set Z.
	 * 
	 * @return {@code true} if this numeric type is integral; {@code false}
	 *             otherwise
	 */
	boolean isIntegral();

	/**
	 * Returns {@code true} if this numeric type is signed.
	 * 
	 * @return {@code true} if this numeric type is signed; {@code false}
	 *             otherwise
	 */
	default boolean isSigned() {
		return !isUnsigned();
	}

	/**
	 * Checks if the two implementations of {@link NumericType} of {@code this}
	 * and {@code other} represent the same type, and can thus be used
	 * interchangeably. For two instances represent the same type, every
	 * {@code isX} method defined by this interface has to return the same
	 * value.
	 * 
	 * @param other the other type
	 * 
	 * @return whether or not the two instances represent the same type
	 */
	default boolean sameNumericTypes(
			NumericType other) {
		return getNBits() == other.getNBits();
	}

	/**
	 * Determines which of the two {@link NumericType}s between {@code this} and
	 * {@code other} is supertype for the other. At first, the size of the two
	 * types is considered, and the larger size takes precedence. Then,
	 * precedence is given to non-integral types, and at last, to signed types.
	 * 
	 * @param other the other type
	 * 
	 * @return the supertype between the two
	 */
	default NumericType supertype(
			NumericType other) {
		if (getNBits() < other.getNBits())
			return other;
		if (getNBits() > other.getNBits())
			return this;

		if (isIntegral() && !other.isIntegral())
			return other;
		if (!isIntegral() && other.isIntegral())
			return this;

		if (isUnsigned() && other.isSigned())
			return other;
		if (isSigned() && other.isUnsigned())
			return this;

		return this; // they are both same-bit same-signed non-integral types
	}

	/**
	 * Computes the set of {@link Type}s representing the common ones among the
	 * given sets. The result is computed as follows:
	 * <ul>
	 * <li>if both arguments have no numeric types among their possible types,
	 * then an empty set is returned</li>
	 * <li>for each pair {@code <t1, t2>} where {@code t1} is a type of
	 * {@code left} and {@code t2} is a type of {@code right}:
	 * <ul>
	 * <li>if {@code t1} is {@link Untyped}, then {@link Untyped#INSTANCE} is
	 * added to the set</li>
	 * <li>if {@code t2} is {@link Untyped}, then {@link Untyped#INSTANCE} is
	 * added to the set</li>
	 * <li>if {@code t1} can be assigned to {@code t2}, then {@code t2} is added
	 * to the set</li>
	 * <li>if {@code t2} can be assigned to {@code t1}, then {@code t1} is added
	 * to the set</li>
	 * <li>if none of the above conditions hold (that is usually a symptom of a
	 * type error), a singleton set containing {@link Untyped#INSTANCE} is
	 * immediately returned</li>
	 * </ul>
	 * </li>
	 * <li>if the set of possible types is not empty, it is returned as-is,
	 * otherwise a singleton set containing {@link Untyped#INSTANCE} is
	 * returned</li>
	 * </ul>
	 * 
	 * @param left  the left-hand side of the operation
	 * @param right the right-hand side of the operation
	 * 
	 * @return the set of possible runtime types
	 */
	public static Set<Type> commonNumericalType(
			Set<Type> left,
			Set<Type> right) {
		Set<Type> lfiltered = left.stream()
				.filter(type -> type.isNumericType() || type.isUntyped())
				.collect(Collectors.toSet());
		Set<Type> rfiltered = right.stream()
				.filter(type -> type.isNumericType() || type.isUntyped())
				.collect(Collectors.toSet());
		if ((lfiltered.isEmpty() || lfiltered.stream().allMatch(Type::isUntyped))
				&& (rfiltered.isEmpty() || rfiltered.stream().allMatch(Type::isUntyped)))
			// if none have numeric types in them,
			// we cannot really compute the
			return Collections.emptySet();

		Set<Type> result = new HashSet<>();
		for (Type t1 : lfiltered)
			for (Type t2 : rfiltered)
				if (t1.isUntyped() && t2.isUntyped())
					result.add(t1);
				else if (t1.isUntyped())
					result.add(t2);
				else if (t2.isUntyped())
					result.add(t1);
				else
					result.add(t1.commonSupertype(t2));

		return result;
	}

	@Override
	default boolean castIsConversion() {
		return true;
	}

}
