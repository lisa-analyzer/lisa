package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CFGDescriptor;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Objects;

/**
 * An identifier outside the current scope of the call, that is, in a method
 * that is in the call stack but not the last one.
 * 
 * @author <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */
public class OutsideScopeIdentifier extends Identifier {
	private Call scope;
	private Identifier id;

	/**
	 * Builds the identifier outside the scope.
	 *
	 * @param id    the current identifier
	 * @param scope the method call that caused the identifier to exit the scope
	 */
	public OutsideScopeIdentifier(Identifier id, Call scope) {
		super(id.getTypes(), scope.toString() + ":" + id.getName(), id.isWeak());
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
		OutsideScopeIdentifier that = (OutsideScopeIdentifier) o;
		return Objects.equals(scope, that.scope);
	}

	@Override
	public SymbolicExpression pushScope(Call scope) {
		return new OutsideScopeIdentifier(this, scope);
	}

	@Override
	public Identifier popScope(Call scope) throws SemanticException {
		if (this.getScope().equals(scope))
			return this.id;
		CFGDescriptor descr1 = scope.getCFG().getDescriptor();
		CFGDescriptor descr2 = this.getScope().getCFG().getDescriptor();
		if (descr1.getFullName().equals(descr2.getFullName()))
			if (descr1.getArgs().length == descr2.getArgs().length)
				return this.id;// FIXME: this check should also consider the
								// parameters
		throw new SemanticException("Unable to pop different scopes");
	}

	private UnresolvedCall.ResolutionStrategy extractStrategy(Call scope) {
		if (scope instanceof UnresolvedCall)
			return ((UnresolvedCall) scope).getStrategy();
		else
			return null;
	}

	/**
	 * Returns the scope of the identifier.
	 * 
	 * @return the scope of the identifier
	 */
	public Call getScope() {
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
}
