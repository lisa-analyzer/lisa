package it.unive.lisa.test.imp.expressions;

import org.apache.commons.lang3.ArrayUtils;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.CFG.ExpressionStates;
import it.unive.lisa.cfg.statement.CFGCall;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.MetaVariableCreator;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.cfg.statement.Statement;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueIdentifier;

public class IMPNewObj extends NativeCall {

	public IMPNewObj(CFG cfg, String sourceFile, int line, int col, Type type, Expression... parameters) {
		super(cfg, sourceFile, line, col, "new[]", type, parameters);
	}

	@Override
	protected <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression[] params)
			throws SemanticException {
		CFGCall call = null; // TODO we need to resolve this to the actual constructor
		// TODO should be runtime type
		HeapAllocation created = new HeapAllocation(getStaticType());
		SymbolicExpression[] fullParams = ArrayUtils.insert(0, params, created);
		AnalysisState<H, V> returned = callGraph.getAbstractResultOf(call, computedState, fullParams);
		return new AnalysisState<>(computedState.getState().lub(returned.getState()), created);
	}
}
