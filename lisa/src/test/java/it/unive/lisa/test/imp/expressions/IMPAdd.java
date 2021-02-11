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
import it.unive.lisa.test.imp.types.StringType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the addition operation ({@code +}). If both operands'
 * dynamic type (according to {@link SymbolicExpression#getDynamicType()}) is a
 * {@link it.unive.lisa.type.StringType} (according to
 * {@link Type#isStringType()}), then this operation translates to a string
 * concatenation of its operands, and its type is {@link StringType}. Otherwise,
 * both operands' types must be instances of {@link NumericType}, and the type
 * of this expression (i.e., a numerical sum) is the common numerical type of
 * its operands, according to the type inference.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPAdd extends BinaryNativeCall {

	/**
	 * Builds the addition.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param left       the left-hand side of this operation
	 * @param right      the right-hand side of this operation
	 */
	public IMPAdd(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col, "+", left, right);
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
		BinaryOperator op;
		if (left.getDynamicType().isStringType() && right.getDynamicType().isStringType())
			op = BinaryOperator.STRING_CONCAT;
		else if ((left.getDynamicType().isNumericType() || left.getDynamicType().isUntyped())
				&& (right.getDynamicType().isNumericType() || right.getDynamicType().isUntyped()))
			op = BinaryOperator.NUMERIC_ADD;
		else
			return entryState.bottom();

		return rightState
				.smallStepSemantics(new BinaryExpression(getRuntimeTypes(), left, right, op), this);
	}
}
