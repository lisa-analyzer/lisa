package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * An event signaling the start of the context transfer from a callee back to
 * the caller during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisOnCallReturnStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		StartEvent {

	private final AnalysisState<A> callState;
	private final AnalysisState<A> calleeRestult;
	private final ProgramPoint call;

	/**
	 * Builds the event.
	 * 
	 * @param callState    the analysis state before the call
	 * @param calleeResult the analysis state computed at the end of the callee
	 * @param call         the call program point
	 */
	public AnalysisOnCallReturnStart(
			AnalysisState<A> callState,
			AnalysisState<A> calleeResult,
			ProgramPoint call) {
		this.callState = callState;
		this.calleeRestult = calleeResult;
		this.call = call;
	}

	/**
	 * Yields the analysis state before the call.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getCallState() {
		return callState;
	}

	/**
	 * Yields the analysis state computed at the end of the callee.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getCalleeRestult() {
		return calleeRestult;
	}

	/**
	 * Yields the call program point.
	 * 
	 * @return the call program point
	 */
	public ProgramPoint getCall() {
		return call;
	}

	@Override
	public String getTarget() {
		return "Analysis: Returning from call " + call;
	}

}
