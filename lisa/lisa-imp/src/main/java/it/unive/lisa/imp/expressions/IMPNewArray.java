package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.imp.types.ArrayType;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * An expression modeling the array allocation operation
 * ({@code new type[...]}). The type of this expression is the {@link Type} of
 * the array's elements. Note that the dimensions of the array are ignored. This
 * expression corresponds to a {@link MemoryAllocation}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPNewArray extends NaryExpression {

	private final boolean staticallyAllocated;

	/**
	 * Builds the array allocation.
	 * 
	 * @param cfg                 the {@link CFG} where this operation lies
	 * @param sourceFile          the source file name where this operation is
	 *                                defined
	 * @param line                the line number where this operation is
	 *                                defined
	 * @param col                 the column where this operation is defined
	 * @param type                the type of the array's elements
	 * @param staticallyAllocated if this allocation is static or not
	 * @param dimensions          the dimensions of the array
	 */
	public IMPNewArray(CFG cfg, String sourceFile, int line, int col, Type type, boolean staticallyAllocated,
			Expression[] dimensions) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), (staticallyAllocated ? "" : "new ") + type + "[]",
				ArrayType.lookup(type, dimensions.length), dimensions);
		this.staticallyAllocated = staticallyAllocated;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> expressionSemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		MemoryAllocation alloc = new MemoryAllocation(getStaticType(), getLocation(), staticallyAllocated);
		AnalysisState<A> sem = state.smallStepSemantics(alloc, this);

		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression loc : sem.getComputedExpressions()) {
			ReferenceType staticType = new ReferenceType(loc.getStaticType());
			HeapReference ref = new HeapReference(staticType, loc, getLocation());
			AnalysisState<A> refSem = sem.smallStepSemantics(ref, this);
			result = result.lub(refSem);
		}

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(staticallyAllocated);
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
		IMPNewArray other = (IMPNewArray) obj;
		return staticallyAllocated == other.staticallyAllocated;
	}

}
