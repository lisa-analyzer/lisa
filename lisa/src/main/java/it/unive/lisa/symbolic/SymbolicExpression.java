package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.SemanticDomain;
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
