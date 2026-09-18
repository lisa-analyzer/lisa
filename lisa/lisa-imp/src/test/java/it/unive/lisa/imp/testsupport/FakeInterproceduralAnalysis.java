package it.unive.lisa.imp.testsupport;

import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Set;

// a hand-rolled fake InterproceduralAnalysis whose only real purpose is to
// hand out the Analysis<UnitLattice, RecordingDomain> wrapping a
// RecordingDomain: none of the other methods (call resolution, fixpoint
// driving, ...) are exercised by testing a single expression's own semantics
// method in isolation, so they are left unimplemented on purpose
public class FakeInterproceduralAnalysis
		implements
		InterproceduralAnalysis<UnitLattice, RecordingDomain> {

	private final Analysis<UnitLattice, RecordingDomain> analysis;

	public FakeInterproceduralAnalysis(
			RecordingDomain domain) {
		this.analysis = new Analysis<>(domain);
	}

	@Override
	public boolean needsCallGraph() {
		return false;
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			EventQueue events,
			Analysis<UnitLattice, RecordingDomain> analysis)
			throws InterproceduralAnalysisException {
		throw new UnsupportedOperationException("not needed for this test");
	}

	@Override
	public Analysis<UnitLattice, RecordingDomain> getAnalysis() {
		return analysis;
	}

	@Override
	public EventQueue getEventQueue() {
		return null;
	}

	@Override
	public void fixpoint(
			AnalysisState<UnitLattice> entryState,
			FixpointConfiguration<UnitLattice, RecordingDomain> conf)
			throws FixpointException {
		throw new UnsupportedOperationException("not needed for this test");
	}

	@Override
	public Collection<AnalyzedCFG<UnitLattice>> getAnalysisResultsOf(
			CFG cfg) {
		throw new UnsupportedOperationException("not needed for this test");
	}

	@Override
	public AnalysisState<UnitLattice> getAbstractResultOf(
			CFGCall call,
			AnalysisState<UnitLattice> entryState,
			ExpressionSet[] parameters,
			StatementStore<UnitLattice> expressions)
			throws SemanticException {
		throw new UnsupportedOperationException("not needed for this test");
	}

	@Override
	public AnalysisState<UnitLattice> getAbstractResultOf(
			OpenCall call,
			AnalysisState<UnitLattice> entryState,
			ExpressionSet[] parameters,
			StatementStore<UnitLattice> expressions)
			throws SemanticException {
		throw new UnsupportedOperationException("not needed for this test");
	}

	@Override
	public Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		throw new UnsupportedOperationException("not needed for this test");
	}

	@Override
	public FixpointResults<UnitLattice> getFixpointResults() {
		throw new UnsupportedOperationException("not needed for this test");
	}

}
