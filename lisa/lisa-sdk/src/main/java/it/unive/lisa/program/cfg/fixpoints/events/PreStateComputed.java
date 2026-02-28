package it.unive.lisa.program.cfg.fixpoints.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An event signaling that the pre-state for a statement has been computed
 * during a fixpoint computation. Depending on the analysis direction, the
 * pre-state might actually be the post-state: we do not distinguish between the
 * two in terms of events for ease of handling.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class PreStateComputed<A extends AbstractLattice<A>>
		extends
		Event
		implements
		FixpointEvent {

	private final Statement node;
	private final AnalysisState<A> result;

	/**
	 * Builds the event.
	 * 
	 * @param node   the statement whose pre-state has been computed
	 * @param result the computed pre-state
	 */
	public PreStateComputed(
			Statement node,
			AnalysisState<A> result) {
		this.node = node;
		this.result = result;
	}

	/**
	 * Yields the statement whose pre-state has been computed.
	 * 
	 * @return the statement
	 */
	public Statement getNode() {
		return node;
	}

	/**
	 * Yields the computed pre-state.
	 * 
	 * @return the pre-state
	 */
	public AnalysisState<A> getResult() {
		return result;
	}

}
