package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A reference to a memory location, identified by its name.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapReference
		extends
		HeapExpression {

	/**
	 * The expression referred by this expression
	 */
	private final SymbolicExpression expression;

	/**
	 * Builds the heap reference.
	 * 
	 * @param staticType the static type of this expression
	 * @param expression the expression that this refers to
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public HeapReference(
			Type staticType,
			SymbolicExpression expression,
			CodeLocation location) {
		super(staticType, location);
		this.expression = expression;
	}

	@Override
	public String toString() {
		return "ref$" + expression.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		return result;
	}

	/**
	 * Yields the referred expression.
	 * 
	 * @return the referred expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
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
		HeapReference other = (HeapReference) obj;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		return true;
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		T l = expression.accept(visitor, params);
		return visitor.visit(this, l, params);
	}

	@Override
	public SymbolicExpression removeTypingExpressions() {
		SymbolicExpression e = expression.removeTypingExpressions();
		if (e == expression)
			return this;
		return new HeapReference(getStaticType(), e, getCodeLocation());
	}

	@Override
	public SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target) {
		if (this.equals(source))
			return target;

		SymbolicExpression e = expression.replace(source, target);
		if (e == expression)
			return this;
		return new HeapReference(getStaticType(), e, getCodeLocation());
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		SymbolicExpression e = expression.pushScope(token, pp);
		if (e == null)
			return null;
		if (e == expression || e.equals(expression))
			return this;
		return new HeapReference(getStaticType(), e, getCodeLocation());
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		SymbolicExpression e = expression.popScope(token, pp);
		if (e == null)
			return null;
		if (e == expression || e.equals(expression))
			return this;
		return new HeapReference(getStaticType(), e, getCodeLocation());
	}

}
