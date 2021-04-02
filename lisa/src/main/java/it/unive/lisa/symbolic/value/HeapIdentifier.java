package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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
	 * @param types the runtime types of this expression
	 * @param name  the name of the identifier
	 * @param weak  whether or not this identifier is weak, meaning that it
	 *                  should only receive weak assignments
	 */
	public HeapIdentifier(ExternalSet<Type> types, String name, boolean weak) {
		super(types, name, weak);
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return this;
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return this;
	}

	@Override
	public String toString() {
		return "hid$" + getName();
	}

}
