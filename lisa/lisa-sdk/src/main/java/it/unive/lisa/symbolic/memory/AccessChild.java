package it.unive.lisa.symbolic.memory;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * Base class for field access expressions ({@code p.f} and {@code p[s]}),
 * representing access to a child memory location reachable from a container.
 * Concrete subclasses are {@link StaticAccess} (compile-time field name) and
 * {@link DynamicAccess} (runtime field name).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class AccessChild
		extends
		MemoryExpression {

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
	public AccessChild(
			Type staticType,
			SymbolicExpression container,
			SymbolicExpression child,
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
	public boolean equals(
			Object obj) {
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

	/**
	 * Creates a new instance of the same concrete subclass with the given
	 * arguments. Used by structural operations ({@link #pushScope},
	 * {@link #popScope}, {@link #replace}, {@link #removeTypingExpressions}) to
	 * preserve the subtype after rebuilding the expression.
	 *
	 * @param staticType the static type
	 * @param container  the container expression
	 * @param child      the child expression
	 * @param location   the code location
	 *
	 * @return a new instance of the same subclass
	 */
	protected abstract AccessChild create(
			Type staticType,
			SymbolicExpression container,
			SymbolicExpression child,
			CodeLocation location);

	@Override
	public SymbolicExpression removeTypingExpressions() {
		SymbolicExpression cont = container.removeTypingExpressions();
		SymbolicExpression ch = child.removeTypingExpressions();
		if (cont == container && ch == child)
			return this;
		return create(getStaticType(), cont, ch, getCodeLocation());
	}

	@Override
	public SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target) {
		if (this.equals(source))
			return target;

		SymbolicExpression cont = container.replace(source, target);
		SymbolicExpression ch = child.replace(source, target);
		if (cont == container && ch == child)
			return this;
		return create(getStaticType(), cont, ch, getCodeLocation());
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		SymbolicExpression e = container.pushScope(token, pp);
		if (e == null)
			return null;
		if (e == container || e.equals(container))
			return this;
		return create(getStaticType(), e, child, getCodeLocation());
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		SymbolicExpression e = container.popScope(token, pp);
		if (e == null)
			return null;
		if (e == container || e.equals(container))
			return this;
		return create(getStaticType(), e, child, getCodeLocation());
	}

}
