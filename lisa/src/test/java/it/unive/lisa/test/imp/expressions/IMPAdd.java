package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.BinaryNativeCall;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.test.imp.types.StringType;
import it.unive.lisa.util.collections.ExternalSet;

public class IMPAdd extends BinaryNativeCall implements BinaryNumericalOperation {

	public IMPAdd(CFG cfg, String sourceFile, int line, int col, Expression left, Expression right) {
		super(cfg, sourceFile, line, col, "+", new Expression[] { left, right });
	}

	@Override
	protected <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> binarySemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression left, SymbolicExpression right)
			throws SemanticException {
		BinaryOperator op;
		ExternalSet<Type> types;
		if (left.getDynamicType().isStringType() && right.getDynamicType().isStringType()) {
			op = BinaryOperator.STRING_CONCAT;
			types = Caches.types().mkSingletonSet(StringType.INSTANCE);
		} else {
			op = BinaryOperator.NUMERIC_ADD;
			types = commonNumericalType(left, right);
		}
		return computedState
				.smallStepSemantics(new BinaryExpression(types, left, right, op));
	}
}
