package it.unive.lisa.cfg.type;

/**
 * Type interface. Any instance of a concrete type, instance of Type, 
 * should be unique and implemented following the singleton design pattern
 * (see for instance {@link Untyped} class).
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public interface Type {
	
	/**
	 * Checks if this is a numeric type.
	 * 
	 * @return true if this is a numeric type; false otherwise.
	 */
	public default boolean isNumeric() {
		return this instanceof NumericType;
	}

	/**
	 * Checks if this is a boolean type
	 * 
	 * @return true if this is a Boolean type; false otherwise.
	 */
	public default boolean isBoolean() {
		return this instanceof BooleanType;
	}
	
	/**
	 * Checks if this is a string type
	 * 
	 * @return true if this is a string type; false otherwise.
	 */
	public default boolean isString() {
		return this instanceof StringType;
	}
	
	/**
	 * Returns true if this is untyped.
	 * 
	 * @return true if this is a untyped; false otherwise.
	 */
	public default boolean isUntyped() {
		return this instanceof Untyped;
	}
	
	/**
	 * Returns this as numeric type.
	 * 
	 * @return this as numeric type if it is numeric; null otherwise.
	 */
	public default NumericType asNumeric() {
		return isNumeric() ? (NumericType) this : null;
	}
	
	/**
	 * Returns this as Boolean type.
	 * 
	 * @return this as Boolean type if it is Boolean; null otherwise.
	 */
	public default BooleanType asBoolean() {
		return isBoolean() ? (BooleanType) this : null;
	}
	
	/**
	 * Returns this as string type.
	 * 
	 * @return this as string type if it is string; null otherwise.
	 */
	public default StringType asString() {
		return isString() ? (StringType) this : null;
	}
}
