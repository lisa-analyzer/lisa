package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.symbolic.ExpressionVisitor;
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
	 * @param types the runtime types of this variable
	 * @param name  the name of the variable
	 */
	public Variable(ExternalSet<Type> types, String name) {
		this(types, name, new Annotations());
	}

	/**
	 * Builds the variable with annotations.
	 * 
	 * @param types       the runtime types of this variable
	 * @param name        the name of the variable
	 * @param annotations the annotations of this variable
	 */
	public Variable(ExternalSet<Type> types, String name, Annotations annotations) {
		super(types, name, false, annotations);
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return new OutOfScopeIdentifier(this, token);
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return null;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
