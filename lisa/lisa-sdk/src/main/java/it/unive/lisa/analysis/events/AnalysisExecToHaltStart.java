package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;

/**
 * An event signaling the start of the transfer of the execution state to the
 * halting state during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisExecToHaltStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		StartEvent {

	private final AnalysisState<A> state;

	/**
	 * Builds the event.
	 * 
	 * @param state the analysis state before the transfer
	 */
	public AnalysisExecToHaltStart(
			AnalysisState<A> state) {
		this.state = state;
	}

	/**
	 * Yields the analysis state before the transfer.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	@Override
	public String getTarget() {
		return "Analysis: Moving state to halt";
	}

}
