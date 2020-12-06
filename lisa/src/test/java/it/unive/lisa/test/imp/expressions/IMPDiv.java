package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.BinaryNativeCall;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.type.NumericType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;

/**
 * An expression modeling the division operation ({@code /}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the common numerical type of its operands, according to
 * {@link BinaryNumericalOperation#commonNumericalType(SymbolicExpression, SymbolicExpression)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPDiv extends BinaryNativeCall implements BinaryNumericalOperation {

	/**
	 * Builds the division.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param left       the left-hand side of this operation
	 * @param right      the right-hand side of this operation
	 */
	public IMPDiv(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col, "/", left, right);
	}

	@Override
	protected <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> binarySemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression left, SymbolicExpression right)
			throws SemanticException {
		// we allow untyped for the type inference phase
		if (!left.getDynamicType().isNumericType() && !left.getDynamicType().isUntyped())
			return computedState.bottom();
		if (!right.getDynamicType().isNumericType() && !right.getDynamicType().isUntyped())
			return computedState.bottom();

		return computedState
				.smallStepSemantics(new BinaryExpression(commonNumericalType(left, right), left, right,
						BinaryOperator.NUMERIC_DIV));
	}
}
