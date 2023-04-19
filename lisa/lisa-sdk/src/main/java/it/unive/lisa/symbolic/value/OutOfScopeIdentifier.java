package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An identifier outside the current scope of the call, that is, in a method
 * that is in the call stack but not the last one.
 * 
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */
public class OutOfScopeIdentifier extends Identifier {

	private final ScopeToken scope;

	private final Identifier id;

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
		super(id.getStaticType(), scope.toString() + ":" + id.getName(), id.isWeak(), id.getAnnotations(), location);
		this.id = id;
		this.scope = scope;
		if (id.hasRuntimeTypes())
			setRuntimeTypes(id.getRuntimeTypes(null));
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
	public Identifier popScope(ScopeToken token) throws SemanticException {
		if (getScope().equals(token))
			return this.id;
		return null;
	}

	@Override
	public boolean isScopedByCall() {
		return scope.getScoper() instanceof Call || (id instanceof OutOfScopeIdentifier && id.isScopedByCall());
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
	public String toString() {
		return this.getName();
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
