package it.unive.lisa.test.imp.expressions;

import java.util.Collection;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.ExpressionStates;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.test.imp.types.BoolType;

public class IMPAnd extends NativeCall {

	public IMPAnd(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col, "&&", BoolType.INSTANCE, new Expression[] { left, right });
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
			throws SemanticException {
		AnalysisState<H, V> result = null;
		for (SymbolicExpression expr1 : params[0])
			for (SymbolicExpression expr2 : params[1]) {
				AnalysisState<H, V> tmp = new AnalysisState<>(computedState.getState(),
						new BinaryExpression(Caches.types().mkSingletonSet(BoolType.INSTANCE), expr1, expr2,
								BinaryOperator.LOGICAL_AND));
				if (result == null)
					result = tmp;
				else
					result = result.lub(tmp);
			}
		return result;
	}
}
