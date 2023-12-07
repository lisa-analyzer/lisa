package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

/**
 * Terminates the execution of the CFG where this statement lies, without
 * returning anything to the caller. For terminating CFGs that must return a
 * value, use {@link Return}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Ret extends Statement {

	/**
	 * Builds the return, happening at the given location in the program.
	 * 
	 * @param cfg      the cfg that this statement belongs to
	 * @param location the location where the statement is defined within the
	 *                     program
	 */
	public Ret(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location);
	}

	@Override
	public boolean stopsExecution() {
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ getClass().getName().hashCode();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	protected int compareSameClass(
			Statement o) {
		return 0; // no extra fields to compare
	}

	@Override
	public final String toString() {
		return "ret";
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		return entryState.smallStepSemantics(new Skip(getLocation()), this);
	}

	@Override
	public <V> boolean accept(
			GraphVisitor<CFG, Statement, Edge, V> visitor,
			V tool) {
		return visitor.visit(tool, getCFG(), this);
	}
}
