package it.unive.lisa.test.imp.expressions;

import java.util.Collection;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapAllocation;

public class IMPNewArray extends NativeCall {

	public IMPNewArray(CFG cfg, String sourceFile, int line, int col, Type type) {
		super(cfg, sourceFile, line, col, "new[]", type);
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
			throws SemanticException {
		return computedState.smallStepSemantics(new HeapAllocation(getRuntimeTypes()));
	}

	@Override
	public <H extends HeapDomain<H>> AnalysisState<H, TypeEnvironment> callTypeInference(
			AnalysisState<H, TypeEnvironment> computedState, CallGraph callGraph,
			Collection<SymbolicExpression>[] params) throws SemanticException {
		return computedState.smallStepSemantics(new HeapAllocation(Caches.types().mkSingletonSet(getStaticType())));
	}
}
