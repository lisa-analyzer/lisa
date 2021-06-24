package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A reference to a memory location, identified by its name.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapReference extends HeapExpression {

	/**
	 * The expression referred by this expression
	 */
	private final SymbolicExpression expression;

	/**
	 * Builds the heap reference.
	 * 
	 * @param types      the runtime types of this expression
	 * @param expression the expression that this refers to
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public HeapReference(ExternalSet<Type> types, SymbolicExpression expression, CodeLocation location) {
		super(types, location);
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
	public boolean equals(Object obj) {
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
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		T l = expression.accept(visitor, params);
		return visitor.visit(this, l, params);
	}
}
