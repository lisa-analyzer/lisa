package it.unive.lisa.symbolic.heap;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * An allocation of a memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapAllocation extends HeapExpression {

	/**
	 * Builds the heap allocation.
	 * 
	 * @param types the runtime types of this expression
	 */
	public HeapAllocation(ExternalSet<Type> types) {
		super(types);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ getClass().getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "new " + getTypes();
	}
}
