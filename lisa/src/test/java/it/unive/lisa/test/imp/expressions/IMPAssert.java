package it.unive.lisa.test.imp.expressions;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ExpressionStore;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Skip;

public class IMPAssert extends Statement {

	private final Expression expression;
	
	public IMPAssert(CFG cfg, String sourceFile, int line, int col, Expression expression) {
		super(cfg, sourceFile, line, col);
		this.expression = expression;
	}

	@Override
	public int setOffset(int offset) {
		return this.offset = offset;
	}

	@Override
	public String toString() {
		return "assert " + expression;
	}
	
	@Override
	public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> typeInference(
			AnalysisState<H, TypeEnvironment> entryState, CallGraph callGraph,
			ExpressionStore<AnalysisState<H, TypeEnvironment>> expressions) throws SemanticException {
		AnalysisState<H, TypeEnvironment> result = expression.typeInference(entryState, callGraph, expressions);
		expressions.put(expression, result);
		if (!expression.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(expression.getMetaVariables());
		return result.smallStepSemantics(new Skip());
	}
	
	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> semantics(
			AnalysisState<H, V> entryState, CallGraph callGraph, ExpressionStore<AnalysisState<H, V>> expressions)
			throws SemanticException {
		AnalysisState<H, V> result = expression.semantics(entryState, callGraph, expressions);
		expressions.put(expression, result);
		if (!expression.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(expression.getMetaVariables());
		return result.smallStepSemantics(new Skip());
	}
}
