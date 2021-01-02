package it.unive.lisa.program.cfg.edge;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.HeapDomain;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A sequential edge connecting two statement. The abstract analysis state does
 * not get modified when traversing this edge.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SequentialEdge extends Edge {

	/**
	 * Builds the edge.
	 * 
	 * @param source      the source statement
	 * @param destination the destination statement
	 */
	public SequentialEdge(Statement source, Statement destination) {
		super(source, destination);
	}

	@Override
	public String toString() {
		return "[ " + getSource() + " ] ---> [ " + getDestination() + " ]";
	}

	@Override
	public <A extends AbstractState<A, H, V>,
			H extends HeapDomain<H>,
			V extends ValueDomain<V>> AnalysisState<A, H, V> traverse(
					AnalysisState<A, H, V> sourceState) {
		return sourceState;
	}

	@Override
	public boolean canBeSimplified() {
		return true;
	}

	@Override
	public SequentialEdge newInstance(Statement source, Statement destination) {
		return new SequentialEdge(source, destination);
	}
}
