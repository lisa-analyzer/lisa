package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Objects;

/**
 * An identifier outside the current scope of the call, that is, in a method
 * that is in the call stack but not the last one.
 * 
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */
public class OutOfScopeIdentifier extends Identifier {
	private ScopeToken scope;
	private Identifier id;

	/**
	 * Builds the identifier outside the scope.
	 *
	 * @param id       the current identifier
	 * @param scope    the method call that caused the identifier to exit the
	 *                     scope
	 * @param location the code location of the statement that has generated
	 *                     this expression
	 */
	public OutOfScopeIdentifier(Identifier id, ScopeToken scope, CodeLocation location) {
		super(id.getTypes(), scope.toString() + ":" + id.getName(), id.isWeak(), location);
		this.id = id;
		this.scope = scope;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		OutOfScopeIdentifier that = (OutOfScopeIdentifier) o;
		return Objects.equals(scope, that.scope);
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return new OutOfScopeIdentifier(this, token, getCodeLocation());
	}

	@Override
	public Identifier popScope(ScopeToken token) throws SemanticException {
		if (getScope().equals(token))
			return this.id;
		// throw new SemanticException("Unable to pop scope '" + token + "' from
		// '" + getScope() + "'");
		return null;
	}

	/**
	 * Returns the scope of the identifier.
	 * 
	 * @return the scope of the identifier
	 */
	public ScopeToken getScope() {
		return this.scope;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), scope);
	}

	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
