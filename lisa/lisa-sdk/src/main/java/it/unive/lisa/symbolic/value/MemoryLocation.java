package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

// TODO tried making this class abstract with isHeap() as an abstract method:
// only AllocationSite (and its Heap/Stack subclasses) actually distinguish
// heap/stack, but MonolithicMemory and TypeBasedMemory instantiate
// MemoryLocation directly, plus 4 test fixtures in EnvironmentTest.
// Going abstract would require introducing a concrete fallback subclass
// (e.g. a GenericMemoryLocation with isHeap() = true) for
// all of them. Left concrete for now with isHeap() defaulting to true below.

/**
 * An identifier of a synthetic program variable that represents a resolved
 * memory location.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class MemoryLocation
		extends
		Identifier {

	private boolean isAllocation = false;

	/**
	 * Builds the memory location.
	 *
	 * @param staticType the static type of this expression
	 * @param name       the name of the location
	 * @param weak       whether or not this identifier is weak, meaning that it
	 *                       should only receive weak assignments
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public MemoryLocation(
			Type staticType,
			String name,
			boolean weak,
			CodeLocation location) {
		super(staticType, name, weak, new Annotations(), location);
	}

	/**
	 * Yields whether this memory location is on the heap ({@code true}) or on
	 * the stack ({@code false}). Defaults to {@code true}.
	 *
	 * @return whether this memory location is on the heap
	 */
	public boolean isHeap() {
		return true;
	}

	@Override
	public String toString() {
		return (isHeap() ? "heap" : "stack") + "[" + (isWeak() ? "w" : "s") + "]:" + getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		// isAllocation is not considered in hashCode, as it is a property
		// that is not relevant for the identity of the memory location
		// (it is just a property that can be used to refine reasoning)
		result = prime * result + (isWeak() ? 1231 : 1237);
		return result;
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
		MemoryLocation other = (MemoryLocation) obj;
		if (isWeak() != other.isWeak())
			return false;
		// isAllocation is not considered in equals, as it is a property
		// that is not relevant for the identity of the memory location
		// (it is just a property that can be used to refine reasoning)
		return true;
	}

	@Override
	public Identifier lub(
			Identifier other)
			throws SemanticException {
		if (!getName().equals(other.getName()))
			throw new SemanticException(
					"Cannot perform the least upper bound between different identifiers: '"
							+ this
							+ "' and '"
							+ other
							+ "'");
		return isWeak() ? this : other;
	}

	@Override
	public boolean canBeScoped() {
		return false;
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp) {
		return this;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return this;
	}

	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		return visitor.visit(this, params);
	}

	/**
	 * Returns whether this memory location is a reference to a region being
	 * freshly allocated or not. Value/type domains can exploit this information
	 * to refine their reasoning.
	 *
	 * @return whether this memory location is an allocation site or not
	 */
	public boolean isAllocation() {
		return isAllocation;
	}

	/**
	 * Sets whether this memory location is a reference to a region being
	 * freshly allocated or not. Value/type domains can exploit this information
	 * to refine their reasoning.
	 *
	 * @param isAllocation whether this memory location is an allocation site or
	 *                         not
	 */
	public void setAllocation(
			boolean isAllocation) {
		this.isAllocation = isAllocation;
	}

	/**
	 * Returns a non-allocation version of this location, that is, a version
	 * where {@link #isAllocation()} returns false.
	 *
	 * @return the non-allocation version of this location
	 */
	public MemoryLocation asNonAllocation() {
		if (!isAllocation)
			return this;
		else
			return new MemoryLocation(getStaticType(), getName(), isWeak(), getCodeLocation());
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
