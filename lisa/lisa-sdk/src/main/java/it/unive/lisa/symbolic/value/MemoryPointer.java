package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * A memory pointer to a heap location.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class MemoryPointer
		extends
		Identifier {

	/**
	 * The heap location memory pointed by this pointer.
	 */
	private final HeapLocation loc;

	/**
	 * Builds a memory pointer with empty annotations.
	 * 
	 * @param staticType the static type of this expression
	 * @param loc        the heap location pointed by this memory pointer
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public MemoryPointer(
			Type staticType,
			HeapLocation loc,
			CodeLocation location) {
		this(staticType, loc, new Annotations(), location);
	}

	/**
	 * Builds a memory pointer.
	 * 
	 * @param staticType  the static type of this expression
	 * @param loc         the heap location pointed by this memory pointer
	 * @param annotations the annotation of this memory pointer
	 * @param location    the code location of the statement that has generated
	 *                        this expression
	 */
	public MemoryPointer(
			Type staticType,
			HeapLocation loc,
			Annotations annotations,
			CodeLocation location) {
		// A pointer identifier is always a strong identifier
		super(staticType, loc.getName(), false, annotations, location);
		this.loc = loc;
	}

	@Override
	public boolean canBeScoped() {
		return true;
	}

	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp) {
		return new OutOfScopeIdentifier(this, token, getCodeLocation());
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
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
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		return visitor.visit(this, params);
	}

	@Override
	public String toString() {
		return "&" + getName();
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
