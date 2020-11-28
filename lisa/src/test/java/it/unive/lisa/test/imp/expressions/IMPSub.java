package it.unive.lisa.test.imp.expressions;

import java.util.Collection;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;

public class IMPSub extends NativeCall {

	public IMPSub(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col, "-", new Expression[] { left, right });
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
			throws SemanticException {
		AnalysisState<H, V> result = null;
		for (SymbolicExpression expr1 : params[0])
			for (SymbolicExpression expr2 : params[1]) {
				AnalysisState<H, V> tmp = new AnalysisState<>(computedState.getState(),
						new BinaryExpression(getRuntimeTypes(), expr1, expr2, BinaryOperator.NUMERIC_SUB));
				if (result == null)
					result = tmp;
				else
					result = result.lub(tmp);
			}
		return result;
	}
}