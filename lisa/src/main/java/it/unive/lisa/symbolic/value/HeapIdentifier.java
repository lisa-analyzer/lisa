package it.unive.lisa.symbolic.value;

import it.unive.lisa.cfg.type.Type;

/**
 * An identifier of a synthetic program variable that represents a resolved
 * memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapIdentifier extends Identifier {

	/**
	 * Builds the identifier.
	 * 
	 * @param type the runtime type of this expression
	 * @param name the name of the identifier
	 */
	public HeapIdentifier(Type type, String name) {
		super(type, name);
	}

	@Override
	public String toString() {
		return "hid$" + getName();
	}

}
