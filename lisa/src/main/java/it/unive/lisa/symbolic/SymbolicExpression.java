package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

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
		return types.reduce(types.first(), (result, t) -> {
			if (result.canBeAssignedTo(t))
				return t;
			if (t.canBeAssignedTo(result))
				return result;
			return t.commonSupertype(result);
		});
	}


	/**
	 * Push a new scope in the call stack caused by calling the method passed as parameter.
	 * This causes the current local variables to be hidden by the method call.
	 *
	 * @param scope the called method
	 * @return the abstract state where the local variables have been hidden
	 */
	public abstract SymbolicExpression pushScope(Call scope);

	/**
	 * Pop the new scope from the call stack caused by calling the method passed as parameter.
	 * This causes that the current local variables to be removed from the state, while the local
	 * variables that were hidden by the call to the given method
	 *
	 * @param scope the called method we are exiting
	 * @return the abstract state where the local variables have been removed, while the variables
	 * 		hidden by the given call are visible again
	 * @throws SemanticException if an error occurs during the computation
	 */
	public abstract SymbolicExpression popScope(Call scope) throws SemanticException;


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
