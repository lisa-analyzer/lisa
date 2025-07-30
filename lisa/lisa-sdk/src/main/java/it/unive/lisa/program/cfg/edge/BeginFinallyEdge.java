package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.statement.Statement;

public class BeginFinallyEdge
		extends
		Edge {

    private final Statement starter;

    /**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public BeginFinallyEdge(
			Statement source,
			Statement destination,
            Statement starter) {
		super(source, destination);
        this.starter = starter;
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((starter == null) ? 0 : starter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        BeginFinallyEdge other = (BeginFinallyEdge) obj;
        if (starter == null) {
            if (other.starter != null)
                return false;
        } else if (!starter.equals(other.starter))
            return false;
        return true;
    }

    @Override
	public String toString() {
		return "[ " 
            + getSource() 
            + " ] -(begin at " 
            + starter.toString()
            + ")-> [ " 
            + getDestination() 
            + " ]";
	}

	@Override
	public String getLabel() {
		return "Begin finally started at " + starter.toString();
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
		return true;
	}

	@Override
	public BeginFinallyEdge newInstance(
			Statement source,
			Statement destination) {
		return new BeginFinallyEdge(source, destination, starter);
	}

}
