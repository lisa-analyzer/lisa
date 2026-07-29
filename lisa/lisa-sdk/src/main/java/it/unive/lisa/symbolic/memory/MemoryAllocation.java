package it.unive.lisa.symbolic.memory;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Objects;

/**
 * An allocation of a memory location, either on the stack or on the heap.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MemoryAllocation
		extends
		MemoryExpression {

	/**
	 * Whether this allocation is on the stack ({@code true}) or heap
	 * ({@code false}).
	 */
	private final boolean isStackAllocation;

	/**
	 * Annotations of this memory allocation.
	 */
	private final Annotations anns;

	/**
	 * Builds the memory allocation.
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
	 * Builds the memory allocation.
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
	 * Builds the memory allocation.
	 *
	 * @param staticType        the static type of this expression
	 * @param location          the code location of the statement that has
	 *                              generated this expression
	 * @param isStackAllocation whether this is a stack allocation
	 */
	public MemoryAllocation(
			Type staticType,
			CodeLocation location,
			boolean isStackAllocation) {
		this(staticType, location, new Annotations(), isStackAllocation);
	}

	/**
	 * Builds the memory allocation.
	 *
	 * @param staticType        the static type of this expression
	 * @param location          the code location of the statement that has
	 *                              generated this expression
	 * @param anns              the annotations of this memory allocation
	 * @param isStackAllocation whether this is a stack allocation
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
	 * Yields whether this allocation is on the stack.
	 *
	 * @return {@code true} if this is a stack allocation
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

	/**
	 * Yields the list of annotations of this expression.
	 *
	 * @return the list of annotations of this expression
	 */
	public Collection<Annotation> getAnnotationList() {
		return anns.getAnnotations();
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

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

}
