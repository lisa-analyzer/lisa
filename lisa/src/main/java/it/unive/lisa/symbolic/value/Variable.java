package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
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
	public SymbolicExpression pushScope(ScopeToken token) {
		return new OutOfScopeIdentifier(this, token);
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		throw new SemanticException("Cannot pop the scope of a non-scoped value identifier");
	}

	@Override
	public String toString() {
		return getName();
	}
}
