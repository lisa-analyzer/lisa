package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;

public class EndFinallyEdge
		extends
		Edge {

	private final int pathIdx;

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public EndFinallyEdge(
			Statement source,
			Statement destination,
			int pathIdx) {
		super(source, destination);
		this.pathIdx = pathIdx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + pathIdx;
		return result;
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
		EndFinallyEdge other = (EndFinallyEdge) obj;
		if (pathIdx != other.pathIdx)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[ "
				+ getSource()
				+ " ] -("
				+ pathIdx
				+ ")-> [ "
				+ getDestination()
				+ " ]";
	}

	@Override
	public String getLabel() {
		return "Finally[" + pathIdx + "]";
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> traverseForward(
					AnalysisState<A> state,
					Analysis<A, D> analysis)
					throws SemanticException {
		// TODO implement the semantics here
		return state;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> traverseBackwards(
					AnalysisState<A> state,
					Analysis<A, D> analysis)
					throws SemanticException {
		return traverseForward(state, analysis);
	}

	@Override
	public boolean isUnconditional() {
		return false;
	}

	@Override
	public boolean isErrorHandling() {
		return false;
	}

	@Override
	public boolean isFinallyRelated() {
		return true;
	}

	@Override
	public EndFinallyEdge newInstance(
			Statement source,
			Statement destination) {
		return new EndFinallyEdge(source, destination, pathIdx);
	}

}
