package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.impl.types.TypeEnvironment;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

public class AccessUnitGlobal extends Expression {

	private final Expression receiver;

	private final Global target;

	public AccessUnitGlobal(CFG cfg, Expression receiver, Global target) {
		this(cfg, null, -1, -1, receiver, target);
	}

	public AccessUnitGlobal(CFG cfg, String sourceFile, int line, int col, Expression receiver, Global target) {
		super(cfg, sourceFile, line, col);
		this.receiver = receiver;
		this.target = target;
		receiver.setParentStatement(this);
	}

	@Override
	public int setOffset(int offset) {
		return this.offset = offset;
	}

	@Override
	public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
		return visitor.visit(tool, getCFG(), this);
	}

	@Override
	public String toString() {
		return receiver + "::" + target.getName();
	}

	private SymbolicExpression getVariable() {
		SymbolicExpression expr;
		if (target.getStaticType().isPointerType())
			// the smallStepSemantics will take care of converting that
			// reference to a variable identifier
			// setting also the identifier as computed expression
			expr = new HeapReference(getRuntimeTypes(), target.getName());
		else
			expr = new ValueIdentifier(getRuntimeTypes(), target.getName());
		return expr;
	}

	@Override
	public <A extends AbstractState<A, H, TypeEnvironment>,
			H extends HeapDomain<H>> AnalysisState<A, H, TypeEnvironment> typeInference(
					AnalysisState<A, H, TypeEnvironment> entryState, CallGraph callGraph,
					StatementStore<A, H, TypeEnvironment> expressions) throws SemanticException {
		AnalysisState<A, H, TypeEnvironment> rec = receiver.typeInference(entryState, callGraph, expressions);
		expressions.put(receiver, rec);

		AnalysisState<A, H, TypeEnvironment> result = null;
		for (SymbolicExpression expr : rec.getComputedExpressions()) {
			AnalysisState<A, H, TypeEnvironment> tmp = rec
					.smallStepSemantics(new AccessChild(getRuntimeTypes(), expr, getVariable()));
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}

		if (!receiver.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(receiver.getMetaVariables());
		setRuntimeTypes(result.getState().getValueState().getLastComputedTypes().getRuntimeTypes());
		return result;
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(AnalysisState<A, H, V> entryState,
					CallGraph callGraph, StatementStore<A, H, V> expressions) throws SemanticException {
		AnalysisState<A, H, V> rec = receiver.semantics(entryState, callGraph, expressions);
		expressions.put(receiver, rec);

		AnalysisState<A, H, V> result = null;
		for (SymbolicExpression expr : rec.getComputedExpressions()) {
			AnalysisState<A, H,
					V> tmp = rec.smallStepSemantics(new AccessChild(getRuntimeTypes(), expr, getVariable()));
			if (result == null)
				result = tmp;
			else
				result = result.lub(tmp);
		}

		if (!receiver.getMetaVariables().isEmpty())
			result = result.forgetIdentifiers(receiver.getMetaVariables());
		return result;
	}

}
