package it.unive.lisa.cfg.statement;

import java.util.Collection;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;

public abstract class UnaryNativeCall extends NativeCall {

	protected UnaryNativeCall(CFG cfg, String constructName, Expression... parameters) {
		super(cfg, constructName, parameters);
	}

	protected UnaryNativeCall(CFG cfg, String sourceFile, int line, int col, String constructName,
			Expression... parameters) {
		super(cfg, sourceFile, line, col, constructName, parameters);
	}

	protected UnaryNativeCall(CFG cfg, String sourceFile, int line, int col, String constructName, Type staticType,
			Expression... parameters) {
		super(cfg, sourceFile, line, col, constructName, staticType, parameters);
	}

	protected UnaryNativeCall(CFG cfg, String constructName, Type staticType, Expression... parameters) {
		super(cfg, constructName, staticType, parameters);
	}

	@Override
	public final <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> callTypeInference(
			AnalysisState<H, TypeEnvironment> computedState, CallGraph callGraph,
			Collection<SymbolicExpression>[] params) throws SemanticException {
		AnalysisState<H, TypeEnvironment> result = null;
		for (SymbolicExpression expr : params[0]) {
			AnalysisState<H, TypeEnvironment> tmp = unarySemantics(computedState, callGraph, expr);
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}

		setRuntimeTypes(result.getState().getValueState().getLastComputedTypes().getRuntimeTypes());
		return result;
	}

	@Override
	public final <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
			throws SemanticException {
		AnalysisState<H, V> result = null;
		for (SymbolicExpression expr : params[0]) {
			AnalysisState<H, V> tmp = unarySemantics(computedState, callGraph, expr);
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}
		return result;
	}

	protected abstract <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> unarySemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, SymbolicExpression expr)
			throws SemanticException;
}
