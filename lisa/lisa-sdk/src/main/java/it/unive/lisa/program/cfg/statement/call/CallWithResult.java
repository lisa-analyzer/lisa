package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.program.cfg.statement.call.assignment.ParameterAssigningStrategy;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;

/**
 * A call that evaluate its result directly.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class CallWithResult extends Call implements MetaVariableCreator {

	/**
	 * Builds the call, happening at the given location in the program. The
	 * {@link EvaluationOrder} of the parameter is
	 * {@link LeftToRightEvaluation}.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where this expression is defined
	 *                              within the program
	 * @param assigningStrategy the {@link ParameterAssigningStrategy} of the
	 *                              parameters of this call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param staticType        the static type of this call
	 * @param parameters        the parameters of this call
	 */
	public CallWithResult(CFG cfg, CodeLocation location, ParameterAssigningStrategy assigningStrategy,
			boolean instanceCall, String qualifier, String targetName, Type staticType, Expression... parameters) {
		super(cfg, location, assigningStrategy, instanceCall, qualifier, targetName, staticType, parameters);
	}

	/**
	 * Builds the call, happening at the given location in the program.
	 * 
	 * @param cfg               the cfg that this expression belongs to
	 * @param location          the location where this expression is defined
	 *                              within the program
	 * @param assigningStrategy the {@link ParameterAssigningStrategy} of the
	 *                              parameters of this call
	 * @param instanceCall      whether or not this is a call to an instance
	 *                              method of a unit (that can be overridden) or
	 *                              not
	 * @param qualifier         the optional qualifier of the call (can be null
	 *                              or empty - see {@link #getFullTargetName()}
	 *                              for more info)
	 * @param targetName        the name of the target of this call
	 * @param order             the evaluation order of the sub-expressions
	 * @param staticType        the static type of this call
	 * @param parameters        the parameters of this call
	 */
	public CallWithResult(CFG cfg, CodeLocation location, ParameterAssigningStrategy assigningStrategy,
			boolean instanceCall, String qualifier, String targetName, EvaluationOrder order, Type staticType,
			Expression... parameters) {
		super(cfg, location, assigningStrategy, instanceCall, qualifier, targetName, order, staticType, parameters);
	}

	/**
	 * Computes an analysis state that abstracts the result of this call when
	 * {@code parameters} are used as actual parameters, and the state when the
	 * call is executed is {@code entryState}.
	 * 
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param entryState      the abstract analysis state when the call is
	 *                            reached
	 * @param parameters      the expressions representing the actual parameters
	 *                            of the call
	 *
	 * @return an abstract analysis state representing the abstract result of
	 *             the cfg call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value, if any
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected abstract <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> compute(
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> entryState,
					ExpressionSet<SymbolicExpression>[] parameters,
					StatementStore<A, H, V> expressions)
					throws SemanticException;

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> expressionSemantics(
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V> state,
					ExpressionSet<SymbolicExpression>[] params,
					StatementStore<A, H, V> expressions)
					throws SemanticException {
		// the stack has to be empty
		state = new AnalysisState<>(state.getState(), new ExpressionSet<>());

		// this will contain only the information about the returned
		// metavariable
		AnalysisState<A, H, V> returned = compute(interprocedural, state, params, expressions);

		if (getStaticType().isVoidType() ||
				(getStaticType().isUntyped() && returned.getComputedExpressions().isEmpty()) ||
				(returned.getComputedExpressions().size() == 1
						&& returned.getComputedExpressions().iterator().next() instanceof Skip))
			// no need to add the meta variable since nothing has been pushed on
			// the stack
			return returned.smallStepSemantics(new Skip(getLocation()), this);

		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : returned.getComputedExpressions())
			// It might be the case it chose a
			// target with void return type
			getMetaVariables().add((Identifier) expr);

		getMetaVariables().add(meta);

		AnalysisState<A, H, V> result = returned.bottom();
		for (SymbolicExpression expr : returned.getComputedExpressions()) {
			// We need to perform this evaluation of the identifier not pushed
			// with the scope since otherwise the value associated with the
			// returned variable would be lost
			AnalysisState<A, H, V> tmp = returned.assign(meta, expr, this);
			result = result.lub(tmp.smallStepSemantics(meta, this));
		}

		return result;
	}
}
