package it.unive.lisa.symbolic.memory;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;

/**
 * Base class for field access expressions ({@code p.f} and {@code p[s]}),
 * representing access to a child memory location reachable from a container.
 * The type of the child is fixed by concrete subclasses through {@code C}:
 * {@link StaticAccess} forces it to be a compile-time constant field name
 * ({@link Variable}), while {@link DynamicAccess} keeps it as a full
 * {@link SymbolicExpression} that must be evaluated at runtime.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <C> the type of the child of this access
 */
public abstract class AccessChild<C>
		extends
		MemoryExpression {

	/**
	 * The expression representing the parent memory location
	 */
	private final SymbolicExpression container;

	/**
	 * The child memory location
	 */
	private final C child;

	/**
	 * Builds the child access.
	 *
	 * @param staticType the static type of this expression
	 * @param container  the expression representing the parent
	 * @param child      the child memory location
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	protected AccessChild(
			Type staticType,
			SymbolicExpression container,
			C child,
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
	 * Yields the child memory location of this access.
	 * 
	 * @return the child
	 */
	public C getChild() {
		return child;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		// TODO null check on container/child can be replaced with
		// Objects.hashCode(container/child)
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
		AccessChild<?> other = (AccessChild<?>) obj;
		// TODO null check on container can be replaced with
		// Objects.equals(container, other.container)?
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		// TODO null check on child can be replaced with Objects.equals(child,
		// other.child)?
		if (child == null) {
			if (other.child != null)
				return false;
		} else if (!child.equals(other.child))
			return false;
		return true;
	}

	/**
	 * Creates a new instance of the same concrete subclass with the given
	 * arguments.
	 *
	 * @param staticType the static type
	 * @param container  the container expression
	 * @param child      the child of this access
	 * @param location   the code location
	 *
	 * @return a new instance of the same subclass
	 */
	protected abstract AccessChild<C> create(
			Type staticType,
			SymbolicExpression container,
			C child,
			CodeLocation location);

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
