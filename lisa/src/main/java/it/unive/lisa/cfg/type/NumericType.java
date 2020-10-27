package it.unive.lisa.cfg.type;

/**
 * Numeric type interface. Any concrete numerical type or numerical sub-interface 
 * should implement/extend this interface.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public interface NumericType extends Type {

	/**
	 * Returns true if this numeric type follows a 8-bits format representation.
	 * 
	 * @return true if this numeric type follows a 8-bits format representation; false otherwise 
	 */
	public boolean is8Bits();
	
	/**
	 * Returns true if this numeric type follows a 32-bits format representation.
	 * 
	 * @return true if this numeric type follows a 32-bits format representation; false otherwise
	 */
	
	public boolean is32Bits();
	
	/**
	 * Returns whether this numeric type follows a 64-bits format representation.
	 * 
	 * @return true if this numeric type follows a 64-bits format representation; false otherwise 
	 */
	public boolean is64its();
	
	/**
	 * Returns true if this numeric type is unsigned.
	 *  
	 * @return true if this numeric type is unsigned; false otherwise
	 */
	public boolean isUnsigned();
	
	/**
	 * Returns true if this numeric type is signed. 
	 *  
	 * @return true if this numeric type is signed; false otherwise
	 */
	public default boolean isSigned() {
		return !isUnsigned();
	}
}
