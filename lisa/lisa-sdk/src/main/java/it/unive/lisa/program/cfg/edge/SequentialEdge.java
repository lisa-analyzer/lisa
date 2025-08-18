package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A sequential edge connecting two statement. The abstract analysis state does
 * not get modified when traversing this edge.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SequentialEdge extends Edge {

	/**
	 * Builds an "empty" edge, meaning that it does not have endpoints.
	 */
	public SequentialEdge() {
		super();
	}

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public SequentialEdge(
			Statement source,
			Statement destination) {
		super(source, destination);
	}

	@Override
	public String toString() {
		return "[ " + getSource() + " ] ---> [ " + getDestination() + " ]";
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> traverseForward(
			AnalysisState<A> state,
			Analysis<A, D> analysis) {
		return state;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> traverseBackwards(
			AnalysisState<A> state,
			Analysis<A, D> analysis)
			throws SemanticException {
		return traverseForward(state, analysis);
	}

	@Override
	public boolean isUnconditional() {
		return true;
	}

	@Override
	public boolean isErrorHandling() {
		return false;
	}

	@Override
	public boolean isFinallyRelated() {
		return false;
	}

	@Override
	public SequentialEdge newInstance(
			Statement source,
			Statement destination) {
		return new SequentialEdge(source, destination);
	}

}
