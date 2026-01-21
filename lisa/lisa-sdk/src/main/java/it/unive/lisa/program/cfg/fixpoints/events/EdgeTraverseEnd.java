package it.unive.lisa.program.cfg.fixpoints.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.edge.Edge;

/**
 * An event signaling the end of the traversal for a given Edge during a
 * fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class EdgeTraverseEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		FixpointEvent,
		EndEvent {

	private final Edge edge;
	private final AnalysisState<A> entryState;
	private final AnalysisState<A> result;

	/**
	 * Builds the event.
	 * 
	 * @param edge       the edge being traversed
	 * @param entryState the entry state for the traversal
	 * @param result     the result state of the traversal
	 */
	public EdgeTraverseEnd(
			Edge edge,
			AnalysisState<A> entryState,
			AnalysisState<A> result) {
		this.edge = edge;
		this.entryState = entryState;
		this.result = result;
	}

	/**
	 * Yields the edge that is being traversed.
	 * 
	 * @return the edge
	 */
	public Edge getEdge() {
		return edge;
	}

	/**
	 * Yields the entry state for the traversal.
	 * 
	 * @return the entry state
	 */
	public AnalysisState<A> getEntryState() {
		return entryState;
	}

	/**
	 * Yields the result state of the traversal.
	 * 
	 * @return the result state
	 */
	public AnalysisState<A> getResult() {
		return result;
	}

	@Override
	public String getTarget() {
		return "Traversal of " + edge;
	}
}
