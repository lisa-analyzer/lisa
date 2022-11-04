package it.unive.lisa.type;

import java.util.Set;

/**
 * Pointer type interface. This can be used to represent whatever type that
 * represents an address to a memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface PointerType extends Type {

	/**
	 * Yields the inner types, that is, the types of the memory region that
	 * variables with this type point to.
	 * 
	 * @return the inner types
	 */
	Set<Type> getInnerTypes();
}
