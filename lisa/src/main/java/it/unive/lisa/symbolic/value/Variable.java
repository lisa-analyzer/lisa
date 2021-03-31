package it.unive.lisa.symbolic.value;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An identifier of a real program variable.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Variable extends Identifier {

	/**
	 * Builds the variable.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name of the variable
	 */
	public Variable(ExternalSet<Type> types, String name) {
		super(types, name, false);
	}

	@Override
	public String toString() {
		return "vid$" + getName();
	}
}
