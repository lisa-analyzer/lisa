package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * An {@link NaryExpression} with a single sub-expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class UnaryExpression extends NaryExpression {

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
	 * @param subExpression the sub-expression of this expression
	 */
	protected UnaryExpression(CFG cfg, CodeLocation location, String constructName,
			Expression subExpression) {
		super(cfg, location, constructName, subExpression);
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
	 * @param subExpression the sub-expression of this expression
	 */
	protected UnaryExpression(CFG cfg, CodeLocation location, String constructName, Type staticType,
			Expression subExpression) {
		super(cfg, location, constructName, staticType, subExpression);
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
	 * @param subExpression the sub-expression of this expression
	 */
	protected UnaryExpression(CFG cfg, CodeLocation location, String constructName, EvaluationOrder order,
			Expression subExpression) {
		super(cfg, location, constructName, order, subExpression);
	}

	/**
	 * Builds the expression, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where the expression is defined within
	 *                          the program
	 * @param constructName the name of the construct represented by this
	 *                          expression
	 * @param staticType    the static type of this expression
	 * @param subExpression the sub-expression of this expression
	 */
	protected UnaryExpression(CFG cfg, CodeLocation location, String constructName, EvaluationOrder order,
			Type staticType, Expression subExpression) {
		super(cfg, location, constructName, order, staticType, subExpression);
	}

	/**
	 * Yields the only sub-expression of this unary expression.
	 * 
	 * @return the only sub-expression
	 */
	public Expression getSubExpression() {
		return getSubExpressions()[0];
	}

	@Override
	public final <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> expressionSemantics(
					AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V>[] computedStates,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		AnalysisState<A, H, V> result = entryState.bottom();
		for (SymbolicExpression expr : params[0])
			result = result.lub(unarySemantics(entryState, interprocedural, computedStates[0], expr));
		return result;
	}

	/**
	 * Computes the semantics of the expression, after the semantics of the
	 * sub-expression has been computed. Meta variables from the sub-expression
	 * will be forgotten after this expression returns.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param entryState      the entry state of this unary expression
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param exprState       the state obtained by evaluating {@code expr} in
	 *                            {@code entryState}
	 * @param expr            the symbolic expressions representing the computed
	 *                            value of the sub-expression of this expression
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this expression
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> unarySemantics(
					AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> exprState,
					SymbolicExpression expr)
					throws SemanticException;
}
