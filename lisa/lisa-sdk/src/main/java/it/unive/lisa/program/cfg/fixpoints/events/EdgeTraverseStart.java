package it.unive.lisa.program.cfg.fixpoints.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.program.cfg.edge.Edge;

/**
 * An event signaling the start of the traversal for a given Edge during a
 * fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class EdgeTraverseStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		FixpointEvent,
		StartEvent {

	private final Edge edge;
	private final AnalysisState<A> entryState;

	/**
	 * Builds the event.
	 * 
	 * @param edge       the edge being traversed
	 * @param entryState the entry state for the traversal
	 */
	public EdgeTraverseStart(
			Edge edge,
			AnalysisState<A> entryState) {
		this.edge = edge;
		this.entryState = entryState;
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

	@Override
	public String getTarget() {
		return "Traversal of " + edge;
	}
}
