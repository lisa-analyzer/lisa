package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
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
public abstract class CallWithResult
		extends
		Call
		implements
		MetaVariableCreator,
		ResolvedCall {

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
	public CallWithResult(
			CFG cfg,
			CodeLocation location,
			CallType callType,
			String qualifier,
			String targetName,
			EvaluationOrder order,
			Type staticType,
			Expression... parameters) {
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
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * 
	 * @return an abstract analysis state representing the abstract result of
	 *             the cfg call. The
	 *             {@link AnalysisState#getComputedExpressions()} will contain
	 *             an {@link Identifier} pointing to the meta variable
	 *             containing the abstraction of the returned value, if any
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public abstract <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> compute(
					AnalysisState<A> entryState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions,
					ExpressionSet[] parameters)
					throws SemanticException;

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					ExpressionSet[] params,
					StatementStore<A> expressions)
					throws SemanticException {
		// the stack has to be empty
		state = new AnalysisState<>(state.getState(), new ExpressionSet(), state.getFixpointInformation());

		// this will contain only the information about the returned
		// metavariable
		AnalysisState<A> returned = compute(state, interprocedural, expressions, params);

		if (this.returnsVoid(returned))
			// no need to add the meta variable since nothing has been pushed on
			// the stack
			return interprocedural.getAnalysis().smallStepSemantics(returned, new Skip(getLocation()), this);

		Identifier meta = getMetaVariable();
		for (SymbolicExpression expr : returned.getComputedExpressions())
			getMetaVariables().add((Identifier) expr);
		getMetaVariables().add(meta);
		return returned;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> backwardSemanticsAux(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					ExpressionSet[] params,
					StatementStore<A> expressions)
					throws SemanticException {
		// TODO implement this when backward analysis will be out of
		// beta
		throw new UnsupportedOperationException();
	}

}
