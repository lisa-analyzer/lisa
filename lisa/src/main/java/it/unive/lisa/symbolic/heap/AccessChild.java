package it.unive.lisa.symbolic.heap;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * An expression that accesses a memory location that is a <i>child</i> of
 * another one, that is, the former is reachable from the latter.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AccessChild extends HeapExpression {

	/**
	 * The expression representing the parent memory location
	 */
	private final SymbolicExpression container;

	/**
	 * The expression representing the child memory location
	 */
	private final SymbolicExpression child;

	/**
	 * Builds the child access.
	 * 
	 * @param types     the runtime types of this expression
	 * @param container the expression representing the parent memory location
	 * @param child     the expression representing the child memory location
	 */
	public AccessChild(ExternalSet<Type> types, SymbolicExpression container, SymbolicExpression child) {
		super(types);
		this.container = container;
		this.child = child;
	}

	/**
	 * Yields the expression representing the parent memory location.
	 * 
	 * @return the container
	 */
	public SymbolicExpression getContainer() {
		return container;
	}

	/**
	 * Yields the expression representing the child memory location.
	 * 
	 * @return the child
	 */
	public SymbolicExpression getChild() {
		return child;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((container == null) ? 0 : container.hashCode());
		result = prime * result + ((child == null) ? 0 : child.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccessChild other = (AccessChild) obj;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (child == null) {
			if (other.child != null)
				return false;
		} else if (!child.equals(other.child))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return container + "->" + child;
	}
}
