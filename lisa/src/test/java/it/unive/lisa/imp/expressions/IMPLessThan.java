package it.unive.lisa.imp.expressions;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.imp.types.BoolType;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.BinaryNativeCall;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.type.NumericType;

/**
 * An expression modeling the less than operation ({@code <}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the {@link BoolType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPLessThan extends BinaryNativeCall {

	/**
	 * Builds the less than.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param left       the left-hand side of this operation
	 * @param right      the right-hand side of this operation
	 */
	public IMPLessThan(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "<", BoolType.INSTANCE, left, right);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> binarySemantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
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
				.smallStepSemantics(new BinaryExpression(Caches.types().mkSingletonSet(BoolType.INSTANCE), left, right,
						BinaryOperator.COMPARISON_LT), this);
	}
}
