package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A heap dereference expression.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class HeapDereference extends HeapExpression {

	/**
	 * The symbolic expression to be dereferenced.
	 */
	private final SymbolicExpression toDeref;

	/**
	 * Builds the heap dereference.
	 * 
	 * @param staticType the static type of this expression
	 * @param toDeref    the expression to be dereference
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public HeapDereference(Type staticType, SymbolicExpression toDeref, CodeLocation location) {
		super(staticType, location);
		this.toDeref = toDeref;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((toDeref == null) ? 0 : toDeref.hashCode());
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
		HeapDereference other = (HeapDereference) obj;
		if (toDeref == null) {
			if (other.toDeref != null)
				return false;
		} else if (!toDeref.equals(other.toDeref))
			return false;
		return true;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		T deref = toDeref.accept(visitor, params);
		return visitor.visit(this, deref, params);
	}

	/**
	 * Yields the expression to be dereferenced.
	 * 
	 * @return the expression to be dereferenced
	 */
	public SymbolicExpression getExpression() {
		return toDeref;
	}

	@Override
	public String toString() {
		return "*(" + toDeref + ")";
	}
}
