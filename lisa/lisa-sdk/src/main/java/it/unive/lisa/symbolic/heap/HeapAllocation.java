package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * An allocation of a memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapAllocation extends HeapExpression {

	/**
	 * If this allocation is statically or dynamically allocated.
	 */
	private final boolean staticallyAllocated;

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
	 * @param staticType          the static type of this expression
	 * @param location            the code location of the statement that has
	 *                                generated this expression
	 * @param staticallyAllocated if this allocation is statically allocated or
	 *                                not
	 */
	public HeapAllocation(Type staticType, CodeLocation location, boolean staticallyAllocated) {
		super(staticType, location);
		this.staticallyAllocated = staticallyAllocated;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(staticallyAllocated);
		return result;
	}

	/**
	 * Yields whether this heap allocation is static or not.
	 * 
	 * @return whether this heap allocation is static or not
	 */
	public boolean isStaticallyAllocated() {
		return staticallyAllocated;
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
		return staticallyAllocated == other.staticallyAllocated;
	}

	@Override
	public String toString() {
		return (staticallyAllocated ? "" : "new ") + getStaticType();
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}
}
