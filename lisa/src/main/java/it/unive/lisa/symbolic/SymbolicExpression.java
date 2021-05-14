package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A symbolic expression that can be evaluated by {@link SemanticDomain}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class SymbolicExpression {

	/**
	 * The runtime types of this expression
	 */
	private final ExternalSet<Type> types;

	/**
	 * Builds the symbolic expression.
	 * 
	 * @param types the runtime types of this expression
	 */
	protected SymbolicExpression(ExternalSet<Type> types) {
		this.types = types;
	}

	/**
	 * Yields the runtime types of this expression.
	 * 
	 * @return the runtime types
	 */
	public final ExternalSet<Type> getTypes() {
		return types;
	}

	/**
	 * Yields the dynamic type of this expression, that is, the most specific
	 * common supertype of all its runtime types (available through
	 * {@link #getTypes()}.
	 * 
	 * @return the dynamic type of this expression
	 */
	public final Type getDynamicType() {
		return types.reduce(types.first(), (result, t) -> result.commonSupertype(t));
	}

	/**
	 * Pushes a new scope, identified by the give token, in the expression. This
	 * causes all {@link Variable}s to become {@link OutOfScopeIdentifier}s
	 * associated with the given token.
	 *
	 * @param token the token identifying the scope to push
	 * 
	 * @return a copy of this expression where the local variables have gone out
	 *             of scope
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract SymbolicExpression pushScope(ScopeToken token) throws SemanticException;

	/**
	 * Pops the scope identified by the given token from the expression. This
	 * causes all the invisible variables (i.e. {@link OutOfScopeIdentifier}s)
	 * mapped to the given scope to become visible (i.e. {@link Variable}s)
	 * again.
	 *
	 * @param token the token of the scope to be restored
	 * 
	 * @return a copy of this expression where the local variables associated
	 *             with the given scope are visible again
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract SymbolicExpression popScope(ScopeToken token) throws SemanticException;

	/**
	 * Accepts an {@link ExpressionVisitor}, visiting this expression
	 * recursively.
	 * 
	 * @param <T>     the type of value produced by the visiting callbacks
	 * @param visitor the visitor
	 * @param params  additional optional parameters to pass to each visiting
	 *                    callback
	 * 
	 * @return the value produced by the visiting operation
	 * 
	 * @throws SemanticException if an error occurs during the visiting
	 */
	public abstract <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SymbolicExpression other = (SymbolicExpression) obj;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@Override
	public abstract String toString();
}
