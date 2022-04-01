package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * An identifier of a synthetic program variable that represents a resolved
 * memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapLocation extends Identifier {

	/**
	 * Builds the heap location.
	 * 
	 * @param staticType the static type of this expression
	 * @param name       the name of the location
	 * @param weak       whether or not this identifier is weak, meaning that it
	 *                       should only receive weak assignments
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public HeapLocation(Type staticType, String name, boolean weak, CodeLocation location) {
		super(staticType, name, weak, location);
	}

	@Override
	public String toString() {
		return "\"heap[" + (isWeak() ? "w" : "s") + "]:" + getName() + "\"";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isWeak() ? 1231 : 1237);
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
		HeapLocation other = (HeapLocation) obj;
		if (isWeak() != other.isWeak())
			return false;
		return true;
	}

	@Override
	public Identifier lub(Identifier other) throws SemanticException {
		if (!getName().equals(other.getName()))
			throw new SemanticException("Cannot perform the least upper bound between different identifiers: '" + this
					+ "' and '" + other + "'");
		return isWeak() ? this : other;
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return this;
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return this;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
