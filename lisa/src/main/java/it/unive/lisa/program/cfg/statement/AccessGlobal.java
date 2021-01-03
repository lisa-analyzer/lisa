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
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

public class AccessGlobal extends Expression {

	private final Unit container;

	private final Global target;

	public AccessGlobal(CFG cfg, Unit container, Global target) {
		this(cfg, null, -1, -1, container, target);
	}

	public AccessGlobal(CFG cfg, String sourceFile, int line, int col, Unit container, Global target) {
		super(cfg, sourceFile, line, col);
		this.container = container;
		this.target = target;
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
		return container.getName() + "::" + target.getName();
	}

	@Override
	public <A extends AbstractState<A, H, TypeEnvironment>,
			H extends HeapDomain<H>> AnalysisState<A, H, TypeEnvironment> typeInference(
					AnalysisState<A, H, TypeEnvironment> entryState, CallGraph callGraph,
					StatementStore<A, H, TypeEnvironment> expressions) throws SemanticException {
		// unit globals are unique, we can directly access those
        AnalysisState<A, H, TypeEnvironment> result = entryState.smallStepSemantics(new HeapReference(getRuntimeTypes(), toString()));
        setRuntimeTypes(result.getState().getValueState().getLastComputedTypes().getRuntimeTypes());
		return result;
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(AnalysisState<A, H, V> entryState,
					CallGraph callGraph, StatementStore<A, H, V> expressions) throws SemanticException {
		// unit globals are unique, we can directly access those
        return entryState.smallStepSemantics(new HeapReference(getRuntimeTypes(), toString()));
	}

}
