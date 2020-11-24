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
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;

public class IMPArrayAccess extends NativeCall {

	public IMPArrayAccess(CFG cfg, String sourceFile, int line, int col, Expression container, Expression id) {
		super(cfg, sourceFile, line, col, "[]", new Expression[] { container, id });
	}

	@Override
	protected <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression[] params)
			throws SemanticException {
		// TODO should be runtime type
		return new AnalysisState<>(computedState.getState(), new AccessChild(getStaticType(), params[0], params[1]));
	}
}
