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

/**
 * An {@link NaryStatement} with exactly three sub-expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class TernaryStatement extends NaryStatement {

	/**
	 * Builds the statement, happening at the given location in the program. The
	 * {@link EvaluationOrder} is {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg           the cfg that this statement belongs to
	 * @param location      the location where the statement is defined within
	 *                          the program
	 * @param constructName the name of the construct represented by this
	 *                          statement
	 * @param left          the first sub-expression of this statement
	 * @param middle        the second sub-expression of this statement
	 * @param right         the third sub-expression of this statement
	 */
	protected TernaryStatement(
			CFG cfg,
			CodeLocation location,
			String constructName,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, constructName, left, middle, right);
	}

	/**
	 * Builds the statement, happening at the given location in the program.
	 * 
	 * @param cfg           the cfg that this statement belongs to
	 * @param location      the location where the statement is defined within
	 *                          the program
	 * @param constructName the name of the construct represented by this
	 *                          statement
	 * @param order         the evaluation order of the sub-expressions
	 * @param left          the first sub-expression of this statement
	 * @param middle        the second sub-expression of this statement
	 * @param right         the third sub-expression of this statement
	 */
	protected TernaryStatement(
			CFG cfg,
			CodeLocation location,
			String constructName,
			EvaluationOrder order,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, constructName, order, left, middle, right);
	}

	/**
	 * Yields the left-most (first) sub-expression of this statement.
	 * 
	 * @return the left-most sub-expression
	 */
	public Expression getLeft() {
		return getSubExpressions()[0];
	}

	/**
	 * Yields the middle (second) sub-expression of this statement.
	 * 
	 * @return the middle sub-expression
	 */
	public Expression getMiddle() {
		return getSubExpressions()[2];
	}

	/**
	 * Yields the right-most (third) sub-expression of this statement.
	 * 
	 * @return the right-most sub-expression
	 */
	public Expression getRight() {
		return getSubExpressions()[2];
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> statementSemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottom();
		for (SymbolicExpression left : params[0])
			for (SymbolicExpression middle : params[1])
				for (SymbolicExpression right : params[2])
					result = result.lub(ternarySemantics(interprocedural, state, left, middle, right, expressions));

		return result;
	}

	/**
	 * Computes the semantics of the statement, after the semantics of the
	 * sub-expressions have been computed. Meta variables from the
	 * sub-expressions will be forgotten after this statement returns.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param state           the state where the statement is to be evaluated
	 * @param left            the symbolic expression representing the computed
	 *                            value of the first sub-expression of this
	 *                            statement
	 * @param middle          the symbolic expression representing the computed
	 *                            value of the second sub-expression of this
	 *                            statement
	 * @param right           the symbolic expression representing the computed
	 *                            value of the third sub-expression of this
	 *                            statement
	 * @param expressions     the cache where analysis states of intermediate
	 *                            expressions are stored and that can be
	 *                            accessed to query for post-states of
	 *                            parameters expressions
	 * 
	 * @return the {@link AnalysisState} representing the abstract result of the
	 *             execution of this statement
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractState<A>> AnalysisState<A> ternarySemantics(
			InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException;
}
