package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
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
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Objects;

/**
 * An expression modeling the array allocation operation
 * ({@code new type[...]}). The type of this expression is the {@link Type} of
 * the array's elements. Note that the dimensions of the array are ignored. This
 * expression corresponds to a {@link MemoryAllocation}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPNewArray
		extends
		NaryExpression {

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
	public IMPNewArray(
			CFG cfg,
			String sourceFile,
			int line,
			int col,
			Type type,
			boolean staticallyAllocated,
			Expression[] dimensions) {
		super(
				cfg,
				new SourceCodeLocation(sourceFile, line, col),
				(staticallyAllocated ? "" : "new ") + type + "[]",
				ArrayType.register(type, dimensions.length),
				dimensions);
		if (dimensions.length != 1)
			throw new UnsupportedOperationException("Multidimensional arrays are not yet supported");
		this.staticallyAllocated = staticallyAllocated;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return Boolean.compare(staticallyAllocated, ((IMPNewArray) o).staticallyAllocated);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Type type = getStaticType();
		ReferenceType reftype = new ReferenceType(type);
		MemoryAllocation creation = new MemoryAllocation(type, getLocation(), staticallyAllocated);
		HeapReference ref = new HeapReference(reftype, creation, getLocation());

		// we start by allocating the memory region
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, creation, this);

		// we create the synthetic variable that will hold the reference to the
		// newly allocated array until it is assigned to a variable
		InstrumentedReceiver array = new InstrumentedReceiver(reftype, true, getLocation());
		AnalysisState<A> tmp = analysis.assign(allocated, array, ref, this);

		// we define the length of the array as a child element
		AccessChild len = new AccessChild(
				Int32Type.INSTANCE,
				array,
				new Variable(Untyped.INSTANCE, "len", getLocation()),
				getLocation());

		AnalysisState<A> lenSt = state.bottomExecution();
		// TODO fix when we'll support multidimensional arrays
		for (SymbolicExpression dim : params[0])
			lenSt = lenSt.lub(analysis.assign(tmp, len, dim, this));

		// we leave the synthetic array in the program variables
		// until it is popped from the stack to keep a reference to the
		// newly created array
		getMetaVariables().add(array);

		// finally, we leave a reference to the newly created array on the stack
		return analysis.smallStepSemantics(lenSt, array, this);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> backwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		// TODO implement this when backward analysis will be out of
		// beta
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(staticallyAllocated);
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
		IMPNewArray other = (IMPNewArray) obj;
		return staticallyAllocated == other.staticallyAllocated;
	}

}
