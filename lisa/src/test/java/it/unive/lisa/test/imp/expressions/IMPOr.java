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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.test.imp.types.BoolType;

public class IMPOr extends NativeCall {

	public IMPOr(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col, "||", BoolType.INSTANCE, new Expression[] { left, right });
	}

	@Override
	protected <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression[] params)
			throws SemanticException {
		return new AnalysisState<>(computedState.getState(),
				new BinaryExpression(BoolType.INSTANCE, params[0], params[1], BinaryOperator.LOGICAL_OR));
	}
}
