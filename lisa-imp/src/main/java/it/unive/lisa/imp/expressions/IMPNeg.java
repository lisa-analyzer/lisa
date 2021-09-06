package it.unive.lisa.imp.expressions;

import java.util.function.UnaryOperator;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.UnaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import sun.tools.tree.UnaryExpression;

/**
 * An expression modeling the numerical negation operation ({@code -}). The
 * operand's type must be instance of {@link NumericType}. The type of this
 * expression is the same as the one of its operand.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPNeg extends UnaryNativeCall {

	/**
	 * Builds the numerical negation.
	 * 
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param sourceFile the source file name where this operation is defined
	 * @param line       the line number where this operation is defined
	 * @param col        the column where this operation is defined
	 * @param expression the operand of this operation
	 */
	public IMPNeg(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, new SourceCodeLocation(sourceFile, line, col), "-", expression);
	}

	@Override
	protected <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(
					AnalysisState<A, H, V> entryState, InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> exprState,
					SymbolicExpression expr)
					throws SemanticException {
		// we allow untyped for the type inference phase
		if (!expr.getDynamicType().isNumericType() && !expr.getDynamicType().isUntyped())
			return entryState.bottom();

		return exprState.smallStepSemantics(
				new UnaryExpression(expr.getTypes(), expr, UnaryOperator.NUMERIC_NEG, getLocation()),
				this);
	}
}
