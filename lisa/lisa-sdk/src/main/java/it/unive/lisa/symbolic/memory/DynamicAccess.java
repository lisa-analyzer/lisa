package it.unive.lisa.symbolic.memory;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A dynamic field access ({@code p[s]}), where the field name is computed at
 * runtime. Domains rewriting this need to evaluate the runtime key expression
 * first, which requires {@code oracle.eval(s)} — not yet fully implemented.
 *
 * @author <a href="mailto:giacomo.boldini@unive.it">Giacomo Boldini</a>
 */
public class DynamicAccess
		extends
		AccessChild {

	/**
	 * Builds the dynamic access.
	 *
	 * @param staticType the static type of this expression
	 * @param container  the expression representing the parent
	 * @param child      the expression representing the runtime field key
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public DynamicAccess(
			Type staticType,
			SymbolicExpression container,
			SymbolicExpression child,
			CodeLocation location) {
		super(staticType, container, child, location);
	}

	// TODO memory framework notation is "container[key]"; kept as "->" for
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
	protected DynamicAccess create(
			Type staticType,
			SymbolicExpression container,
			SymbolicExpression child,
			CodeLocation location) {
		return new DynamicAccess(staticType, container, child, location);
	}

}
