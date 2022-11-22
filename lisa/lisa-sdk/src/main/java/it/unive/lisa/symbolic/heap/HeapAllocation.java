package it.unive.lisa.symbolic.heap;

import java.util.Objects;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.type.Type;

/**
 * An allocation of a memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapAllocation extends HeapExpression {

	/**
	 * If this allocation is statically or dynamically allocated. 
	 */
	private final boolean isStaticalltAllocated;
	
	
	/**
	 * Builds the heap allocation.
	 * 
	 * @param staticType the static type of this expression
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public HeapAllocation(Type staticType, CodeLocation location) {
		this(staticType, location, false);
	}
	
	/**
	 * Builds the heap allocation.
	 * 
	 * @param staticType the static type of this expression
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public HeapAllocation(Type staticType, CodeLocation location, boolean isStaticalltAllocated) {
		super(staticType, location);
		this.isStaticalltAllocated = isStaticalltAllocated;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(isStaticalltAllocated);
		return result;
	}

	public boolean isStaticallyAllocated() {
		return isStaticalltAllocated;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		HeapAllocation other = (HeapAllocation) obj;
		return isStaticalltAllocated == other.isStaticalltAllocated;
	}

	@Override
	public String toString() {
		return (isStaticalltAllocated ? "" : "new ") + getStaticType();
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
