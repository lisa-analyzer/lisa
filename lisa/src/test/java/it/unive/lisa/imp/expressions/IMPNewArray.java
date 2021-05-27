package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.imp.types.ArrayType;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the array allocation operation
 * ({@code new type[...]}). The type of this expression is the {@link Type} of
 * the array's elements. Note that the dimensions of the array are ignored. This
 * expression corresponds to a {@link HeapAllocation}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPNewArray extends NativeCall {

	/**
	 * Builds the array allocation.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param type       the type of the array's elements
	 * @param dimensions the dimensions of the array
	 */
	public IMPNewArray(CFG cfg, String sourceFile, int line, int col, Type type, Expression[] dimensions) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "new " + type + "[]",
				ArrayType.lookup(type, dimensions.length),
				dimensions);
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V>[] computedStates,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		// it corresponds to the analysis state after the evaluation of all the
		// parameters of this call
		// (the semantics of this call does not need information about the
		// intermediate analysis states)
		AnalysisState<A, H, V> lastPostState = computedStates.length == 0 ? entryState : computedStates[computedStates.length - 1];
		AnalysisState<A, H, V> sem = lastPostState.smallStepSemantics(new HeapAllocation(getRuntimeTypes()), this);

		AnalysisState<A, H, V> result = entryState.bottom();
		
		for (SymbolicExpression loc : sem.getComputedExpressions()) 
			if (loc instanceof HeapLocation) {
				AnalysisState<A, H, V> smallStepSemantics = sem.smallStepSemantics(new HeapReference(loc.getTypes(), (HeapLocation) loc), this);
				result = result.lub(smallStepSemantics);
			}

		return result;
	}
}
