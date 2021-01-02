package it.unive.lisa.symbolic.value;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * An identifier of a real program variable.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ValueIdentifier extends Identifier {

	/**
	 * Builds the identifier.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name of the identifier
	 */
	public ValueIdentifier(ExternalSet<Type> types, String name) {
		super(types, name, false);
	}

	@Override
	public String toString() {
		return "vid$" + getName();
	}
}
