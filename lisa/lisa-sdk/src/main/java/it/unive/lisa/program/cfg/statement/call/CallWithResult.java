package it.unive.lisa.program.cfg.statement.call;

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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;

/**
 * A call that evaluate its result directly.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class CallWithResult extends Call implements MetaVariableCreator, ResolvedCall {

	/**
	 * Builds the call, happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where this expression is defined within
	 *                       the program
	 * @param callType   the call type of this call
	 * @param qualifier  the optional qualifier of the call (can be null or
	 *                       empty - see {@link #getFullTargetName()} for more
	 *                       info)
	 * @param targetName the name of the target of this call
	 * @param order      the evaluation order of the sub-expressions
	 * @param staticType the static type of this call
	 * @param parameters the parameters of this call
	 */
	public CallWithResult(CFG cfg, CodeLocation location, CallType callType, String qualifier, String targetName,
			EvaluationOrder order, Type staticType, Expression... parameters) {
		super(cfg, location, callType, qualifier, targetName, order, staticType, parameters);
	}

	/**
	 * Computes an analysis state that abstracts the result of this call when
	 * {@code parameters} are used as actual parameters, and the state when the
	 * call is executed is {@code entryState}.
	 * 
	 * @param entryState      the abstract analysis state when the call is
	 *                            reached
	 * @param interprocedural the interprocedural analysis of the program to
	 *                            analyze
	 * @param expressions     the cache where analysis states of intermediate
	 *                            expressions must be stored
	 * @param parameters      the expressions representing the actual parameters
	 *                            of the call
	 * @param <A>             the type of {@link AbstractState}
	 * @param <H>             the type of the {@link HeapDomain}
	 * @param <V>             the type of the {@link ValueDomain}
	 * @param <T>             the type of {@link TypeDomain}
	 * 
	 * @return an abstract analysis state representing the abstract result of
	 *             the cfg call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value, if any
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> compute(
					AnalysisState<A, H, V, T> entryState,
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					StatementStore<A, H, V, T> expressions,
					ExpressionSet<SymbolicExpression>[] parameters)
					throws SemanticException;

	@Override
	public <A extends AbstractState<A, H, V, T>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> AnalysisState<A, H, V, T> expressionSemantics(
					InterproceduralAnalysis<A, H, V, T> interprocedural,
					AnalysisState<A, H, V, T> state,
					ExpressionSet<SymbolicExpression>[] params,
					StatementStore<A, H, V, T> expressions)
					throws SemanticException {
		// the stack has to be empty
		state = new AnalysisState<>(state.getState(), new ExpressionSet<>(), state.getAliasing());

		// this will contain only the information about the returned
		// metavariable
		AnalysisState<A, H, V, T> returned = compute(state, interprocedural, expressions, params);

		if (interprocedural.returnsVoid(this, returned))
			// no need to add the meta variable since nothing has been pushed on
			// the stack
			return returned.smallStepSemantics(new Skip(getLocation()), this);

		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : returned.getComputedExpressions())
			getMetaVariables().add((Identifier) expr);
		getMetaVariables().add(meta);
		return returned;
	}
}
