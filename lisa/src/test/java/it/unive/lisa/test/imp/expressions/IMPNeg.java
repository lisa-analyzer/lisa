package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.ExpressionStates;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;

public class IMPNeg extends NativeCall {
	
	public IMPNeg(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, sourceFile, line, col, "-", new Expression[] { expression });
	}

	@Override
	protected <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression[] params)
			throws SemanticException {
		return new AnalysisState<>(computedState.getState(),
				// TODO should be runtime type
				new UnaryExpression(getStaticType(), params[0], UnaryOperator.NUMERIC_NEG));
	}
}
