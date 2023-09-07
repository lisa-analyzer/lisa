package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
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
	 * @param order         the evaluation order of the sub-expressions
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
	 * @param order         the evaluation order of the sub-expressions
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
	public <A extends AbstractState<A>> AnalysisState<A> expressionSemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			ExpressionSet<SymbolicExpression>[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression expr : params[0])
			result = result.lub(unarySemantics(interprocedural, state, expr, expressions));
		return result;
	}

	/**
	 * Computes the semantics of the expression, after the semantics of the
	 * sub-expression has been computed. Meta variables from the sub-expression
	 * will be forgotten after this expression returns.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param state           the state where the expression is to be evaluated
	 * @param expr            the symbolic expressions representing the computed
	 *                            value of the sub-expression of this expression
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
	public abstract <A extends AbstractState<A>> AnalysisState<A> unarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException;
}
