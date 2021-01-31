package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.test.imp.types.ArrayType;
import it.unive.lisa.type.Type;
import java.util.Collection;

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
		super(cfg, sourceFile, line, col, "new[]", ArrayType.lookup(type, dimensions.length));
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph, AnalysisState<A, H, V>[] computedStates,
					Collection<SymbolicExpression>[] params)
					throws SemanticException {
		// it corresponds to the analysis state after the evaluation of all the
		// parameters of this call
		// (the semantics of this call does not need information about the
		// intermediate analysis states)
		AnalysisState<A, H, V> lastPostState = computedStates[computedStates.length - 1];
		return lastPostState.smallStepSemantics(new HeapAllocation(getRuntimeTypes()), this);
	}
}
