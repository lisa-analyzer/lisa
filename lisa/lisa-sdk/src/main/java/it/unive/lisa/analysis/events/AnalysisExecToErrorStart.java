package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;

/**
 * An event signaling the start of the transfer of the execution state to an
 * error state during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisExecToErrorStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		StartEvent {

	private final AnalysisState<A> state;
	private final Error error;

	/**
	 * Builds the event.
	 * 
	 * @param state the analysis state before the transfer
	 * @param error the error being transferred to
	 */
	public AnalysisExecToErrorStart(
			AnalysisState<A> state,
			Error error) {
		this.state = state;
		this.error = error;
	}

	/**
	 * Yields the analysis state before the transfer.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	/**
	 * Yields the error being transferred to.
	 * 
	 * @return the error
	 */
	public Error getError() {
		return error;
	}

	@Override
	public String getTarget() {
		return "Analysis: Moving execution state to error " + error;
	}
}
