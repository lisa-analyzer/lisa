package it.unive.lisa.symbolic.value;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An identifier of a synthetic program variable that represents a resolved
 * memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapLocation extends Identifier {

	/**
	 * Builds the heap location.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name of the location
	 * @param weak  whether or not this identifier is weak, meaning that it
	 *                  should only receive weak assignments
	 */
	public HeapLocation(ExternalSet<Type> types, String name, boolean weak) {
		super(types, name, weak);
	}

	@Override
	public String toString() {
		return "hid$" + getName();
	}

}
