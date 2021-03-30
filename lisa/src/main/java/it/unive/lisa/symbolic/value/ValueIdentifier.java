package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

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
	public SymbolicExpression pushScope(Call scope) {
		return new OutsideScopeIdentifier(this, scope);
	}

	@Override
	public SymbolicExpression popScope(Call scope) throws SemanticException {
		throw new SemanticException("Cannot pop the scope of a non-scoped value identifier");
	}

	@Override
	public String toString() {
		return "vid$" + getName();
	}
}
