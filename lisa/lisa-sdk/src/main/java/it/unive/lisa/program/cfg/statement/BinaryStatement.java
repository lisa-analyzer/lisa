package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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
 * An {@link NaryStatement} with exactly two sub-expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class BinaryStatement
		extends
		NaryStatement {

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
	 * @param right         the second sub-expression of this statement
	 */
	protected BinaryStatement(
			CFG cfg,
			CodeLocation location,
			String constructName,
			Expression left,
			Expression right) {
		super(cfg, location, constructName, left, right);
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
	 * @param right         the second sub-expression of this statement
	 */
	protected BinaryStatement(
			CFG cfg,
			CodeLocation location,
			String constructName,
			EvaluationOrder order,
			Expression left,
			Expression right) {
		super(cfg, location, constructName, order, left, right);
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
	 * Yields the right-most (second) sub-expression of this expression.
	 * 
	 * @return the right-most sub-expression
	 */
	public Expression getRight() {
		return getSubExpressions()[1];
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression left : params[0])
			for (SymbolicExpression right : params[1])
				result = result.lub(fwdBinarySemantics(interprocedural, state, left, right, expressions));

		return result;
	}

	/**
	 * Computes the forward semantics of the statement, after the semantics of
	 * the sub-expressions have been computed. Meta variables from the
	 * sub-expressions will be forgotten after this statement returns.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param state           the state where the statement is to be evaluated
	 * @param left            the symbolic expression representing the computed
	 *                            value of the first sub-expression of this
	 *                            statement
	 * @param right           the symbolic expression representing the computed
	 *                            value of the second sub-expression of this
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
	public abstract <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException;

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> backwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression left : params[0])
			for (SymbolicExpression right : params[1])
				result = result.lub(bwdBinarySemantics(interprocedural, state, left, right, expressions));

		return result;
	}

	/**
	 * Computes the backward semantics of the statement, after the semantics of
	 * the sub-expressions have been computed. Meta variables from the
	 * sub-expressions will be forgotten after this statement returns. By
	 * default, this method delegates to
	 * {@link #fwdBinarySemantics(InterproceduralAnalysis, AnalysisState, SymbolicExpression, SymbolicExpression, StatementStore)},
	 * as it is fine for most atomic statements. One should redefine this method
	 * if a statement's semantics is composed of a series of smaller operations.
	 * 
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param state           the state where the statement is to be evaluated
	 * @param left            the symbolic expression representing the computed
	 *                            value of the first sub-expression of this
	 *                            statement
	 * @param right           the symbolic expression representing the computed
	 *                            value of the second sub-expression of this
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> bwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		return fwdBinarySemantics(interprocedural, state, left, right, expressions);
	}

}
