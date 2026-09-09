package it.unive.lisa.program.cfg.statement.evaluation;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.List;

/**
 * An {@link it.unive.lisa.program.cfg.statement.Expression} whose forward and
 * backward semantics record their own invocation into a shared, ordered log
 * instead of computing anything, so that tests can assert on the exact order in
 * which an {@link EvaluationOrder} visits a set of sub-expressions.
 */
public class RecordingExpression
		extends
		it.unive.lisa.program.cfg.statement.Expression {

	private final String name;
	private final List<String> log;

	public RecordingExpression(
			CFG cfg,
			String name,
			List<String> log) {
		super(cfg, cfg.getDescriptor().getLocation(), Untyped.INSTANCE);
		this.name = name;
		this.log = log;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions) {
		log.add("fwd:" + name);
		return entryState;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> backwardSemantics(
			AnalysisState<A> exitState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions) {
		log.add("bwd:" + name);
		return exitState;
	}

	@Override
	protected int compareSameClass(
			Statement o) {
		return name.compareTo(((RecordingExpression) o).name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public <V> boolean accept(
			GraphVisitor<CFG, Statement, Edge, V> visitor,
			V tool) {
		return true;
	}

}
