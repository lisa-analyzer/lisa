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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.type.NumericType;

/**
 * An expression modeling the subtraction operation ({@code -}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the common numerical type of its operands, according to the type
 * inference.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPSub extends BinaryNativeCall {

	/**
	 * Builds the subtraction.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param left       the left-hand side of this operation
	 * @param right      the right-hand side of this operation
	 */
	public IMPSub(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col, "-", left, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					AnalysisState<A, H, V> entryState, CallGraph callGraph,
					AnalysisState<A, H, V> leftState,
					SymbolicExpression left,
					AnalysisState<A, H, V> rightState,
					SymbolicExpression right)

					throws SemanticException {
		// we allow untyped for the type inference phase
		if (!left.getDynamicType().isNumericType() && !left.getDynamicType().isUntyped())
			return entryState.bottom();
		if (!right.getDynamicType().isNumericType() && !right.getDynamicType().isUntyped())
			return entryState.bottom();

		return rightState
				.smallStepSemantics(new BinaryExpression(getRuntimeTypes(), left, right,
						BinaryOperator.NUMERIC_SUB), this);
	}
}
