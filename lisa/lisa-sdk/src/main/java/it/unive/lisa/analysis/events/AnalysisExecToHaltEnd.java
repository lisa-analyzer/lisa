package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;

/**
 * An event signaling the end of the transfer of the execution state to the
 * halting state during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisExecToHaltEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent {

	private final AnalysisState<A> state;
	private final AnalysisState<A> result;

	/**
	 * Builds the event.
	 * 
	 * @param state  the analysis state before the transfer
	 * @param result the analysis state after the transfer
	 */
	public AnalysisExecToHaltEnd(
			AnalysisState<A> state,
			AnalysisState<A> result) {
		this.state = state;
		this.result = result;
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
	 * Yields the analysis state after the transfer.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getResult() {
		return result;
	}

	@Override
	public String getTarget() {
		return "Analysis: Moving state to halt";
	}

}
