package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * A memory pointer to a heap location.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class MemoryPointer extends Identifier {

	/**
	 * The heap location memory pointed by this pointer.
	 */
	private final HeapLocation loc;

	/**
	 * Builds a memory pointer with empty annotations.
	 * 
	 * @param types    the runtime types of this expression
	 * @param loc      the heap location pointed by this memory pointer
	 * @param location the code location of the statement that has generated
	 *                     this expression
	 */
	public MemoryPointer(ExternalSet<Type> types, HeapLocation loc, CodeLocation location) {
		this(types, loc, new Annotations(), location);
	}

	/**
	 * Builds a memory pointer.
	 * 
	 * @param types       the runtime types of this expression
	 * @param loc         the heap location pointed by this memory pointer
	 * @param annotations the annotation of this memory pointer
	 * @param location    the code location of the statement that has generated
	 *                        this expression
	 */
	public MemoryPointer(ExternalSet<Type> types, HeapLocation loc, Annotations annotations, CodeLocation location) {
		// A pointer identifier is always a strong identifier
		super(types, loc.getName(), false, annotations, location);
		this.loc = loc;
	}

	@Override
	public SymbolicExpression pushScope(ScopeToken token) {
		return new OutOfScopeIdentifier(this, token, getCodeLocation());
	}

	@Override
	public SymbolicExpression popScope(ScopeToken token) throws SemanticException {
		return null;
	}

	/**
	 * Yields the heap location pointed by this pointer.
	 * 
	 * @return the heap location pointed by this pointer
	 */
	public HeapLocation getReferencedLocation() {
		return loc;
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
		MemoryPointer other = (MemoryPointer) obj;
		if (loc == null) {
			if (other.loc != null)
				return false;
		} else if (!loc.equals(other.loc))
			return false;
		return true;
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, params);
	}

	@Override
	public String toString() {
		return "&" + getName();
	}
}
