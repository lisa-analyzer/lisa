package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * An identifier of a real program variable.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Variable extends Identifier {

	/**
	 * Builds the variable.
	 * 
	 * @param staticType the static type of this variable
	 * @param name       the name of the variable
	 * @param location   the code location of the statement that has generated
	 *                       this variable
	 */
	public Variable(Type staticType, String name, CodeLocation location) {
		this(staticType, name, new Annotations(), location);
	}

	/**
	 * Builds the variable with annotations.
	 * 
	 * @param staticType  the static type of this variable
	 * @param name        the name of the variable
	 * @param annotations the annotations of this variable
	 * @param location    the code location of the statement that has generated
	 *                        this variable
	 */
	public Variable(Type staticType, String name, Annotations annotations, CodeLocation location) {
		super(staticType, name, false, annotations, location);
	}

	@Override
	public boolean canBeScoped() {
		return true;
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return new OutOfScopeIdentifier(this, token, getCodeLocation());
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
