package it.unive.lisa.type;

/**
 * Numeric type interface. Any concrete numerical type or numerical
 * sub-interface should implement/extend this interface.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public interface NumericType extends Type {

	/**
	 * Returns {@code true} if this numeric type follows a 8-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 8-bits format
	 *             representation; {@code false} otherwise
	 */
	boolean is8Bits();

	/**
	 * Returns {@code true} if this numeric type follows a 16-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 16-bits format
	 *             representation; {@code false} otherwise
	 */
	boolean is16Bits();

	/**
	 * Returns {@code true} if this numeric type follows a 32-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 32-bits format
	 *             representation; {@code false} otherwise
	 */
	boolean is32Bits();

	/**
	 * Returns {@code true} if this numeric type follows a 64-bits format
	 * representation.
	 * 
	 * @return {@code true} if this numeric type follows a 64-bits format
	 *             representation; {@code false} otherwise
	 */
	boolean is64Bits();

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
}
