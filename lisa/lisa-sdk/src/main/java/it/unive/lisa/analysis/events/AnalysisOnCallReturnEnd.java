package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.ProgramPoint;

/**
 * An event signaling the end of the context transfer from a callee back to the
 * caller during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisOnCallReturnEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent {

	private final AnalysisState<A> callState;
	private final AnalysisState<A> calleeResult;
	private final AnalysisState<A> result;
	private final ProgramPoint call;

	/**
	 * Builds the event.
	 * 
	 * @param callState    the analysis state before the call
	 * @param calleeResult the analysis state computed at the end of the callee
	 * @param result       the analysis state after the context transfer
	 * @param call         the call program point
	 */
	public AnalysisOnCallReturnEnd(
			AnalysisState<A> callState,
			AnalysisState<A> calleeResult,
			AnalysisState<A> result,
			ProgramPoint call) {
		this.callState = callState;
		this.calleeResult = calleeResult;
		this.result = result;
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
	public AnalysisState<A> getCalleeResult() {
		return calleeResult;
	}

	/**
	 * Yields the analysis state after the context transfer.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getResult() {
		return result;
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
