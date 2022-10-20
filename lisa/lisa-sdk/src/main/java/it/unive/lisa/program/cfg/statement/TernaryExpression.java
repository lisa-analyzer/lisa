package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * An {@link NaryExpression} with exactly three sub-expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class TernaryExpression extends NaryExpression {

	/**
	 * Builds the untyped expression, happening at the given location in the
	 * program. The static type of this expression is {@link Untyped}. The
	 * {@link EvaluationOrder} is {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the program
	 * @param constructName the name of the construct represented by this
	 *                          expression
	 * @param left          the first sub-expression of this expression
	 * @param middle        the second sub-expression of this expression
	 * @param right         the third sub-expression of this expression
	 */
	protected TernaryExpression(CFG cfg, CodeLocation location, String constructName,
			Expression left, Expression middle, Expression right) {
		super(cfg, location, constructName, left, middle, right);
	}

	/**
	 * Builds the expression, happening at the given location in the program.
	 * The {@link EvaluationOrder} is {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the program
	 * @param constructName the name of the construct represented by this
	 *                          expression
	 * @param staticType    the static type of this expression
	 * @param left          the first sub-expression of this expression
	 * @param middle        the second sub-expression of this expression
	 * @param right         the third sub-expression of this expression
	 */
	protected TernaryExpression(CFG cfg, CodeLocation location, String constructName, Type staticType,
			Expression left, Expression middle, Expression right) {
		super(cfg, location, constructName, staticType, left, middle, right);
	}

	/**
	 * Builds the untyped expression, happening at the given location in the
	 * program. The static type of this expression is {@link Untyped}.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the program
	 * @param constructName the name of the construct represented by this
	 *                          expression
	 * @param order         the evaluation order of the sub-expressions
	 * @param left          the first sub-expression of this expression
	 * @param middle        the second sub-expression of this expression
	 * @param right         the third sub-expression of this expression
	 */
	protected TernaryExpression(CFG cfg, CodeLocation location, String constructName, EvaluationOrder order,
			Expression left, Expression middle, Expression right) {
		super(cfg, location, constructName, order, left, middle, right);
	}

	/**
	 * Builds the expression, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the program
	 * @param constructName the name of the construct represented by this
	 *                          expression
	 * @param order         the evaluation order of the sub-expressions
	 * @param staticType    the static type of this expression
	 * @param left          the first sub-expression of this expression
	 * @param middle        the second sub-expression of this expression
	 * @param right         the third sub-expression of this expression
	 */
	protected TernaryExpression(CFG cfg, CodeLocation location, String constructName, EvaluationOrder order,
			Type staticType, Expression left, Expression middle, Expression right) {
		super(cfg, location, constructName, order, staticType, left, middle, right);
	}

	/**
	 * Yields the left-most (first) sub-expression of this expression.
	 * 
	 * @return the left-most sub-expression
	 */
	public Expression getLeft() {
		return getSubExpressions()[0];
	}

	/**
	 * Yields the middle (second) sub-expression of this expression.
	 * 
	 * @return the middle sub-expression
	 */
	public Expression getMiddle() {
		return getSubExpressions()[2];
	}

	/**
	 * Yields the right-most (third) sub-expression of this expression.
	 * 
	 * @return the right-most sub-expression
	 */
	public Expression getRight() {
		return getSubExpressions()[2];
	}

	@Override
	public final <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> expressionSemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					ExpressionSet<SymbolicExpression>[] params,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		AnalysisState<A, H, V, T> result = state.bottom();
		for (SymbolicExpression left : params[0])
			for (SymbolicExpression middle : params[1])
				for (SymbolicExpression right : params[2])
					result = result.lub(ternarySemantics(interprocedural, state, left, middle, right, expressions));

		return result;
	}

	/**
	 * Computes the semantics of the expression, after the semantics of the
	 * sub-expressions have been computed. Meta variables from the
	 * sub-expressions will be forgotten after this expression returns.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param <T>             the type of {@link TypeDomain}
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param state           the state where the expression is to be evaluated
	 * @param left            the symbolic expression representing the computed
	 *                            value of the first sub-expression of this
	 *                            expression
	 * @param middle          the symbolic expression representing the computed
	 *                            value of the second sub-expression of this
	 *                            expression
	 * @param right           the symbolic expression representing the computed
	 *                            value of the third sub-expression of this
	 *                            expression
	 * @param expressions     the cache where analysis states of intermediate
	 *                            expressions are stored and that can be
	 *                            accessed to query for post-states of
	 *                            parameters expressions
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this expression
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected abstract <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> ternarySemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					SymbolicExpression left,
					SymbolicExpression middle,
					SymbolicExpression right,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException;
}
