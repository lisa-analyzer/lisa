package it.unive.lisa.program.language.scoping;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A default implementation of {@link ScopingStrategy} that pushes/pops the
 * scope on the whole state using
 * {@link AnalysisState#pushScope(ScopeToken, ProgramPoint)} and
 * {@link AnalysisState#popScope(ScopeToken, ProgramPoint)}.
 */
public class DefaultScopingStrategy implements ScopingStrategy {

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Pair<AnalysisState<A>, ExpressionSet[]> scope(
			CFGCall call,
			ScopeToken scope,
			AnalysisState<A> state,
			Analysis<A, D> analysis,
			ExpressionSet[] actuals)
			throws SemanticException {
		ExpressionSet[] locals = new ExpressionSet[actuals.length];
		AnalysisState<A> callState = state.pushScope(scope, call);
		for (int i = 0; i < actuals.length; i++)
			locals[i] = actuals[i].pushScope(scope, call);
		return Pair.of(callState, locals);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> unscope(
			CFGCall call,
			ScopeToken scope,
			AnalysisState<A> state,
			Analysis<A, D> analysis)
			throws SemanticException {
		if (call.returnsVoid(state))
			return state.popScope(scope, call);

		Identifier meta = (Identifier) call.getMetaVariable().pushScope(scope, call);

		if (state.getComputedExpressions().isEmpty()) {
			// a return value is expected, but nothing is left on the stack
			PushInv inv = new PushInv(meta.getStaticType(), call.getLocation());
			return analysis.assign(state, meta, inv, call).popScope(scope, call);
		}

		AnalysisState<A> tmp = state.bottom();
		for (SymbolicExpression ret : state.getComputedExpressions())
			tmp = tmp.lub(analysis.assign(state, meta, ret, call));

		return tmp.popScope(scope, call);
	}

}
