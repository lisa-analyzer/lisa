package it.unive.lisa.symbolic;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.cfg.type.Type;

/**
 * A symbolic expression that can be evaluated by {@link SemanticDomain}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class SymbolicExpression {

	/**
	 * The runtime type of this expression
	 */
	private final Type type;

	/**
	 * Builds the symbolic expression.
	 * 
	 * @param type the runtime type of this expression
	 */
	protected SymbolicExpression(Type type) {
		this.type = type;
	}

	/**
	 * Yields the runtime type of this expression.
	 * 
	 * @return the runtime type
	 */
	public final Type getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public abstract String toString();
}
