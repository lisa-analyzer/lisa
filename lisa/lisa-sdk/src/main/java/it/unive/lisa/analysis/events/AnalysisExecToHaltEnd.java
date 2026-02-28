package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.EvaluationEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.ProgramPoint;

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
		EndEvent,
		EvaluationEvent<AnalysisState<A>, AnalysisState<A>> {

	private final ProgramPoint pp;
	private final AnalysisState<A> state;
	private final AnalysisState<A> result;

	/**
	 * Builds the event.
	 * 
	 * @param pp     the program point where the transfer happens
	 * @param state  the analysis state before the transfer
	 * @param result the analysis state after the transfer
	 */
	public AnalysisExecToHaltEnd(
			ProgramPoint pp,
			AnalysisState<A> state,
			AnalysisState<A> result) {
		this.pp = pp;
		this.state = state;
		this.result = result;
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

	@Override
	public String getTarget() {
		return "Analysis: Moving state to halt";
	}

}
