package it.unive.lisa.program.cfg.statement.evaluation;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * The order of evaluation of the sub-expressions of an {@link NaryExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface EvaluationOrder {

	/**
	 * Assuming that {@code pos} is the index of a sub-expression, yields the
	 * index of the sub-expression that has to be evaluated before the given
	 * one.
	 * 
	 * @param pos the index of a sub-expression
	 * @param len the total number of sub-expressions
	 * 
	 * @return the previous index (a negative value means that there is no
	 *             previous sub-expression, that is, {@code pos} is the first
	 *             that needs to be evaluated)
	 */
	int previous(int pos, int len);

	/**
	 * Assuming that {@code pos} is the index of a sub-expression, yields the
	 * index of the sub-expression that has to be evaluated after the given one.
	 * 
	 * @param pos the index of a sub-expression
	 * @param len the total number of sub-expressions
	 * 
	 * @return the next index (a negative value means that there is no next
	 *             sub-expression, that is, {@code pos} is the last that needs
	 *             to be evaluated)
	 */
	int next(int pos, int len);

	/**
	 * Yields the index of the first sub-expression that has to be evaluated.
	 * 
	 * @param len the total number of sub-expressions
	 * 
	 * @return the index of the first expression to evaluate
	 */
	int first(int len);

	/**
	 * Yields the index of the last sub-expression that has to be evaluated.
	 * 
	 * @param len the total number of sub-expressions
	 * 
	 * @return the index of the last expression to evaluate
	 */
	int last(int len);

	/**
	 * Evaluates the given sub-expressions according to this order. This method
	 * will fill {@code computed} and {@code subStates} such that
	 * {@code subStates[i] = subExpressions[i].semantics(); computed[i] = subStates[i].computedExpressions},
	 * while also setting the runtime types for the expressions left on the
	 * stack.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param <T>             the type of {@link TypeDomain}
	 * @param subExpressions  the sub-expressions to evaluate
	 * @param entryState      the state to use as starting point for the
	 *                            evaluation
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param expressions     the cache where analysis states of intermediate
	 *                            expressions must be stored (this method is
	 *                            responsible for storing the results of the
	 *                            sub-expressions)
	 * @param computed        an array containing, for each sub-expression, the
	 *                            symbolic expressions produced by its
	 *                            evaluation
	 * 
	 * @return the last computed state, where the source expression can be
	 *             evaluated
	 * 
	 * @throws SemanticException if something goes wrong during the evaluation
	 */
	<A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> evaluate(
					Expression[] subExpressions,
					AnalysisState<A, H, V, T> entryState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions,
					ExpressionSet<SymbolicExpression>[] computed)
					throws SemanticException;
}
