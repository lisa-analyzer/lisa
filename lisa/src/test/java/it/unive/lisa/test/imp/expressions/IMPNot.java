package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.UnaryNativeCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.test.imp.types.BoolType;

public class IMPNot extends UnaryNativeCall {

	public IMPNot(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, sourceFile, line, col, "!", BoolType.INSTANCE, new Expression[] { expression });
	}

	@Override
	protected <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> unarySemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression expr) throws SemanticException {
		return computedState.smallStepSemantics(
				new UnaryExpression(Caches.types().mkSingletonSet(BoolType.INSTANCE), expr, UnaryOperator.LOGICAL_NOT));
	}
}
