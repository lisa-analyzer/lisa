package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.BinaryNativeCall;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;

/**
 * An expression modeling a field access operation ({@code object.field}). The
 * type of this expression is the one of the resolved field.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPFieldAccess extends BinaryNativeCall {

	/**
	 * Builds the field access.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param container  the expression representing the object reference that
	 *                       will receive the access
	 * @param id         the expression representing the accessed field
	 */
	public IMPFieldAccess(CFG cfg, String sourceFile, int line, int col, Expression container, Expression id) {
		super(cfg, sourceFile, line, col, ".", container, id);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					AnalysisState<A, H, V> computedState, CallGraph callGraph, SymbolicExpression left,
					SymbolicExpression right)
					throws SemanticException {
		if (!left.getDynamicType().isPointerType() && !left.getDynamicType().isUntyped())
			return computedState.bottom();
		// it is not possible to detect the correct type of the field without
		// resolving it. we rely on the rewriting that will happen inside heap
		// domain to translate this into a variable that will have its correct
		// type
		return computedState.smallStepSemantics(new AccessChild(getRuntimeTypes(), left, right));
	}
}
