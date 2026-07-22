package it.unive.lisa.symbolic.memory;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A static field access ({@code p.f}), where the field name is known at compile
 * time. Domains rewrite this into an {@link AllocationSite} by appending the
 * (constant) field name to the container's allocation site.
 *
 * @author <a href="mailto:giacomo.boldini@unive.it">Giacomo Boldini</a>
 */
public class StaticAccess
		extends
		AccessChild {

	/**
	 * Builds the static access.
	 *
	 * @param staticType the static type of this expression
	 * @param container  the expression representing the parent
	 * @param child      the expression representing the (constant) field
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public StaticAccess(
			Type staticType,
			SymbolicExpression container,
			SymbolicExpression child,
			CodeLocation location) {
		super(staticType, container, child, location);
	}

	// TODO memory framework notation is "container.field"; kept as "->" for
	// compatibility with old tests
	@Override
	public String toString() {
		return getContainer() + "->" + getChild();
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		T cont = getContainer().accept(visitor, params);
		T ch = getChild().accept(visitor, params);
		return visitor.visit(this, cont, ch, params);
	}

	@Override
	protected StaticAccess create(
			Type staticType,
			SymbolicExpression container,
			SymbolicExpression child,
			CodeLocation location) {
		return new StaticAccess(staticType, container, child, location);
	}

}
