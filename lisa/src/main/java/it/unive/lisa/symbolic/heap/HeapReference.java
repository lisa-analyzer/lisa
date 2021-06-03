package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.SemanticException;
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
	 * The name representing the memory location
	 */
	private final SymbolicExpression loc;

	/**
	 * Builds the heap reference.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name that identifies the memory location
	 */
	public HeapReference(ExternalSet<Type> types, SymbolicExpression loc) {
		super(types);
		this.loc = loc;
	}

	@Override
	public String toString() {
		return "ref$" + loc.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
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
		HeapReference other = (HeapReference) obj;
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		return true;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		T l = loc.accept(visitor, params);
		return visitor.visit(this, l, params);
	}
}