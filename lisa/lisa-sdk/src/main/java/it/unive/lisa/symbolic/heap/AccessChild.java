package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

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
	 * @param staticType the static type of this expression
	 * @param container  the expression representing the parent
	 * @param child      the expression representing the child
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public AccessChild(Type staticType, SymbolicExpression container, SymbolicExpression child,
			CodeLocation location) {
		super(staticType, location);
		this.container = container;
		this.child = child;
	}

	/**
	 * Yields the expression representing the parent.
	 * 
	 * @return the container
	 */
	public SymbolicExpression getContainer() {
		return container;
	}

	/**
	 * Yields the expression representing the child.
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

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		T cont = container.accept(visitor, params);
		T ch = child.accept(visitor, params);
		return visitor.visit(this, cont, ch, params);
	}
}
