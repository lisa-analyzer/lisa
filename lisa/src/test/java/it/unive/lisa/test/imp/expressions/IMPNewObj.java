package it.unive.lisa.test.imp.expressions;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.ArrayUtils;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.callgraph.CallGraph;
import it.unive.lisa.cfg.CFG;
import it.unive.lisa.cfg.statement.Call;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.cfg.statement.NativeCall;
import it.unive.lisa.cfg.statement.UnresolvedCall;
import it.unive.lisa.cfg.statement.Variable;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapAllocation;

public class IMPNewObj extends NativeCall {

	public IMPNewObj(CFG cfg, String sourceFile, int line, int col, Type type, Expression... parameters) {
		super(cfg, sourceFile, line, col, "new", type, parameters);
	}

	@Override
	public <H extends HeapDomain<H>, V extends ValueDomain<V>> AnalysisState<H, V> callSemantics(
			AnalysisState<H, V> computedState, CallGraph callGraph, Collection<SymbolicExpression>[] params)
			throws SemanticException {
		HeapAllocation created = new HeapAllocation(getRuntimeTypes());
		
		// we need to add the receiver to the parameters
		Variable paramThis = new Variable(getCFG(), getSourceFile(), getLine(), getCol(), "this", getStaticType());
		Expression[] fullExpressions = ArrayUtils.insert(0, getParameters(), paramThis);
		Collection<SymbolicExpression>[] fullParams = ArrayUtils.insert(0, params, Collections.singleton(created));
		
		UnresolvedCall call = new UnresolvedCall(getCFG(), getSourceFile(), getLine(), getCol(), getStaticType().toString(), fullExpressions);
		Call resolved = callGraph.resolve(call);
		return resolved.callSemantics(computedState, callGraph, fullParams);
	}
}
