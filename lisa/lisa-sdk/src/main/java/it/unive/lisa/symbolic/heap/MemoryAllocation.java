package it.unive.lisa.symbolic.heap;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * An allocation of a memory location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MemoryAllocation extends HeapExpression {

	/**
	 * If this allocation is allocated in the stack.
	 */
	private final boolean isStackAllocation;

	/**
	 * Annotations of this memory allocation.
	 */
	private final Annotations anns;

	/**
	 * Builds the heap allocation.
	 * 
	 * @param staticType the static type of this expression
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public MemoryAllocation(
			Type staticType,
			CodeLocation location) {
		this(staticType, location, false);
	}

	/**
	 * Builds the heap allocation.
	 * 
	 * @param staticType the static type of this expression
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 * @param anns       the annotations of this memory allocation
	 */
	public MemoryAllocation(
			Type staticType,
			CodeLocation location,
			Annotations anns) {
		this(staticType, location, anns, false);
	}

	/**
	 * Builds the heap allocation.
	 * 
	 * @param staticType        the static type of this expression
	 * @param location          the code location of the statement that has
	 *                              generated this expression
	 * @param isStackAllocation if this allocation is allocated in the stack
	 */
	public MemoryAllocation(
			Type staticType,
			CodeLocation location,
			boolean isStackAllocation) {
		this(staticType, location, new Annotations(), isStackAllocation);
	}

	/**
	 * Builds the heap allocation.
	 * 
	 * @param staticType        the static type of this expression
	 * @param location          the code location of the statement that has
	 *                              generated this expression
	 * @param anns              the annotations of this memory allocation
	 * @param isStackAllocation if this allocation is allocated in the stack
	 */
	public MemoryAllocation(
			Type staticType,
			CodeLocation location,
			Annotations anns,
			boolean isStackAllocation) {
		super(staticType, location);
		this.isStackAllocation = isStackAllocation;
		this.anns = anns;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(anns, isStackAllocation);
		return result;
	}

	/**
	 * Yields whether this memory allocation is allocated in the stack.
	 * 
	 * @return whether this memory allocation is allocated in the stack
	 */
	public boolean isStackAllocation() {
		return isStackAllocation;
	}

	/**
	 * Yields the annotations of this expression.
	 * 
	 * @return the annotations of this expression
	 */
	public Annotations getAnnotations() {
		return anns;
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
		MemoryAllocation other = (MemoryAllocation) obj;
		return Objects.equals(anns, other.anns) && isStackAllocation == other.isStackAllocation;
	}

	@Override
	public String toString() {
		return (isStackAllocation ? "" : "new ") + getStaticType();
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		return visitor.visit(this, params);
	}

	@Override
	public SymbolicExpression removeTypingExpressions() {
		return this;
	}

	@Override
	public SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target) {
		if (this.equals(source))
			return target;
		return this;
	}

}
