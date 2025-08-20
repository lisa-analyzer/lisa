package it.unive.lisa.type;

/**
 * Pointer type interface. This can be used to represent whatever type that
 * represents an address to a memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface PointerType
		extends
		Type {

	/**
	 * Yields the inner type, that is, the type of the memory region that
	 * variables with this type point to.
	 * 
	 * @return the inner types
	 */
	Type getInnerType();

}
