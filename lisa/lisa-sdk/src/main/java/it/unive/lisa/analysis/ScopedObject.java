package it.unive.lisa.analysis;

/**
 * An object that can react to the introduction or removal of scopes, modifying
 * the variables currently in view. Scoping happens through
 * {@link #pushScope(ScopeToken)} and {@link #popScope(ScopeToken)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of this object, returned by push and pop
 *                operations
 */
public interface ScopedObject<T> {

	/**
	 * Pushes a new scope, identified by the give token, in this object. This
	 * causes all variables not associated with a scope (and thus visible) to be
	 * mapped to the given scope and hidden away, until the scope is popped with
	 * {@link #popScope(ScopeToken)}.
	 *
	 * @param token the token identifying the scope to push
	 * 
	 * @return a copy of this object where the local unscoped variables have
	 *             been hidden
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T pushScope(
			ScopeToken token)
			throws SemanticException;

	/**
	 * Pops the scope identified by the given token from this object. This
	 * causes all the visible variables (i.e. that are not mapped to a scope) to
	 * be removed, while the local variables that were associated to the given
	 * scope token (and thus hidden) will become visible again.
	 *
	 * @param token the token of the scope to be restored
	 * 
	 * @return a copy of this object where the local variables have been
	 *             removed, while the variables mapped to the given scope are
	 *             visible again
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T popScope(
			ScopeToken token)
			throws SemanticException;
}
