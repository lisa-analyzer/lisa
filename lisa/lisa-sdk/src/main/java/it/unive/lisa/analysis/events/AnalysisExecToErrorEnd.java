package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.EvaluationEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * An event signaling the end of the transfer of the execution state to an error
 * state during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisExecToErrorEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent,
		EvaluationEvent<AnalysisState<A>, AnalysisState<A>> {

	private final ProgramPoint pp;
	private final AnalysisState<A> state;
	private final AnalysisState<A> result;
	private final Error error;

	/**
	 * Builds the event.
	 * 
	 * @param pp     the program point where the transfer happens
	 * @param state  the analysis state before the transfer
	 * @param result the analysis state after the transfer
	 * @param error  the error being transferred to
	 */
	public AnalysisExecToErrorEnd(
			ProgramPoint pp,
			AnalysisState<A> state,
			AnalysisState<A> result,
			Error error) {
		this.pp = pp;
		this.state = state;
		this.result = result;
		this.error = error;
	}

	@Override
	public ProgramPoint getProgramPoint() {
		return pp;
	}

	@Override
	public AnalysisState<A> getPreState() {
		return state;
	}

	@Override
	public AnalysisState<A> getPostState() {
		return result;
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
