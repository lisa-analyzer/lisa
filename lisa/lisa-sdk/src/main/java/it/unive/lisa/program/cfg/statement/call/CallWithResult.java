package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.MetaVariableCreator;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.type.Type;

/**
 * A call to one or more of the CFGs under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class CallWithResult extends Call implements MetaVariableCreator {

	/**
	 * Builds the CFG call, happening at the given location in the program.
	 * 
	 * @param cfg        the cfg that this expression belongs to
	 * @param location   the location where this expression is defined within
	 *                       the source file. If unknown, use {@code null}
	 * @param staticType the static type of this call
	 * @param parameters the parameters of this call
	 */
	public CallWithResult(CFG cfg, CodeLocation location, Type staticType, Expression... parameters) {
		super(cfg, location, staticType, parameters);
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
					ExpressionSet<SymbolicExpression>[] parameters) throws SemanticException;

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> callSemantics(
					AnalysisState<A, H, V> entryState,
					InterproceduralAnalysis<A, H, V> interprocedural,
					AnalysisState<A, H, V>[] computedStates,
					ExpressionSet<SymbolicExpression>[] params)
					throws SemanticException {
		// it corresponds to the analysis state after the evaluation of all the
		// parameters of this call, it is the entry state if this call has no
		// parameters (the semantics of this call does not need information
		// about the intermediate analysis states)
		AnalysisState<A, H, V> callState = computedStates.length == 0
				? entryState
				: computedStates[computedStates.length - 1];
		// the stack has to be empty
		callState = new AnalysisState<>(callState.getState(), new ExpressionSet<>());

		// this will contain only the information about the returned
		// metavariable
		AnalysisState<A, H, V> returned = compute(interprocedural, callState, params);

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
