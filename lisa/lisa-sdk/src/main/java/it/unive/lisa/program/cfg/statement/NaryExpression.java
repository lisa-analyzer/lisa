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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A generic expression with {@code n} sub-expressions.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class NaryExpression extends Expression {

	/**
	 * The sub-expressions of this expression
	 */
	private final Expression[] subExpressions;

	/**
	 * The name of this expression
	 */
	private final String constructName;

	/**
	 * The evaluation order of the sub-expressions
	 */
	private final EvaluationOrder order;

	/**
	 * Builds the expression, happening at the given location in the program.
	 * The static type of this expression is {@link Untyped}. The
	 * {@link EvaluationOrder} is {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg            the cfg that this expression belongs to
	 * @param location       the location where the expression is defined within
	 *                           the program
	 * @param constructName  the name of the construct represented by this
	 *                           expression
	 * @param subExpressions the sub-expressions to be evaluated left-to-right
	 */
	protected NaryExpression(CFG cfg, CodeLocation location, String constructName, Expression... subExpressions) {
		this(cfg, location, constructName, LeftToRightEvaluation.INSTANCE, Untyped.INSTANCE, subExpressions);
	}

	/**
	 * Builds the expression, happening at the given location in the program.
	 * The static type of this expression is {@link Untyped}.
	 * 
	 * @param cfg            the cfg that this expression belongs to
	 * @param location       the location where the expression is defined within
	 *                           the program
	 * @param constructName  the name of the construct represented by this
	 *                           expression
	 * @param order          the evaluation order of the sub-expressions
	 * @param subExpressions the sub-expressions to be evaluated left-to-right
	 */
	protected NaryExpression(CFG cfg, CodeLocation location, String constructName, EvaluationOrder order,
			Expression... subExpressions) {
		this(cfg, location, constructName, order, Untyped.INSTANCE, subExpressions);
	}

	/**
	 * Builds the expression, happening at the given location in the program.
	 * The {@link EvaluationOrder} is {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg            the cfg that this expression belongs to
	 * @param location       the location where the expression is defined within
	 *                           the program
	 * @param constructName  the name of the construct represented by this
	 *                           expression
	 * @param staticType     the static type of this expression
	 * @param subExpressions the sub-expressions to be evaluated left-to-right
	 */
	protected NaryExpression(CFG cfg, CodeLocation location, String constructName, Type staticType,
			Expression... subExpressions) {
		this(cfg, location, constructName, LeftToRightEvaluation.INSTANCE, staticType, subExpressions);
	}

	/**
	 * Builds the expression, happening at the given location in the program.
	 * 
	 * @param cfg            the cfg that this expression belongs to
	 * @param location       the location where the expression is defined within
	 *                           the program
	 * @param constructName  the name of the construct represented by this
	 *                           expression
	 * @param order          the evaluation order of the sub-expressions
	 * @param staticType     the static type of this expression
	 * @param subExpressions the sub-expressions to be evaluated left-to-right
	 */
	protected NaryExpression(CFG cfg, CodeLocation location, String constructName, EvaluationOrder order,
			Type staticType, Expression... subExpressions) {
		super(cfg, location, staticType);
		Objects.requireNonNull(subExpressions, "The array of sub-expressions of an expression cannot be null");
		for (int i = 0; i < subExpressions.length; i++)
			Objects.requireNonNull(subExpressions[i],
					"The " + i + "-th sub-expression of an expression cannot be null");
		Objects.requireNonNull(constructName, "The name of the native construct of an expression cannot be null");
		Objects.requireNonNull(order, "The evaluation order of an expression cannot be null");
		this.constructName = constructName;
		this.order = order;
		this.subExpressions = subExpressions;
		for (Expression param : subExpressions)
			param.setParentStatement(this);
	}

	/**
	 * Yields the name of the native construct represented by this expression.
	 * 
	 * @return the name of the construct
	 */
	public final String getConstructName() {
		return constructName;
	}

	/**
	 * Yields the sub-expressions of this expression, to be evaluated
	 * left-to-right.
	 * 
	 * @return the sub-expressions
	 */
	public final Expression[] getSubExpressions() {
		return subExpressions;
	}

	/**
	 * Yields the {@link EvaluationOrder} of the sub-expressions.
	 * 
	 * @return the evaluation order
	 */
	public EvaluationOrder getOrder() {
		return order;
	}

	@Override
	public int setOffset(int offset) {
		this.offset = offset;
		int off = offset;
		for (Expression sub : subExpressions)
			off = sub.setOffset(off + 1);
		return off;
	}

	@Override
	public Statement getStatementEvaluatedBefore(Statement other) {
		int len = subExpressions.length;
		if (other == this)
			return len == 0 ? null : subExpressions[order.last(len)];

		for (int i = 0; i < len; i++)
			if (subExpressions[i] == other)
				if (i == order.first(len))
					return null;
				else
					return subExpressions[order.previous(i, len)];
		return null;
	}

	@Override
	public final <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		for (Expression sub : subExpressions)
			if (!sub.accept(visitor, tool))
				return false;
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public String toString() {
		return constructName + "(" + StringUtils.join(getSubExpressions(), ", ") + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((constructName == null) ? 0 : constructName.hashCode());
		result = prime * result + Arrays.hashCode(subExpressions);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof NaryExpression))
			return false;
		NaryExpression other = (NaryExpression) obj;
		if (constructName == null) {
			if (other.constructName != null)
				return false;
		} else if (!constructName.equals(other.constructName))
			return false;
		if (!Arrays.equals(subExpressions, other.subExpressions))
			return false;
		return true;
	}

	/**
	 * Semantics of an n-ary expression is evaluated by computing the semantics
	 * of its sub-expressions, from left to right, using the analysis state from
	 * each sub-expression's computation as entry state for the next one. Then,
	 * the semantics of the expression itself is evaluated.<br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> semantics(
					AnalysisState<A, H, V, T> entryState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		ExpressionSet<SymbolicExpression>[] computed = new ExpressionSet[subExpressions.length];

		AnalysisState<A, H, V,
				T> eval = order.evaluate(subExpressions, entryState, interprocedural, expressions, computed);
		AnalysisState<A, H, V, T> result = expressionSemantics(interprocedural, eval, computed, expressions);

		for (Expression sub : subExpressions)
			if (!sub.getMetaVariables().isEmpty())
				result = result.forgetIdentifiers(sub.getMetaVariables());
		return result;
	}

	/**
	 * Computes the semantics of the expression, after the semantics of all
	 * sub-expressions have been computed. Meta variables from the
	 * sub-expressions will be forgotten after this call returns.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param <T>             the type of {@link TypeDomain}
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param state           the state where the expression is to be evaluated
	 * @param params          the symbolic expressions representing the computed
	 *                            values of the sub-expressions of this
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
	public abstract <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> expressionSemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					ExpressionSet<SymbolicExpression>[] params, StatementStore<A, H, V, T> expressions)
					throws SemanticException;
}
