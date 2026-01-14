package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An event signaling the end of the transfer of throwers during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisTransferThrowersEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent {

	private final AnalysisState<A> state;
	private final AnalysisState<A> result;
	private final Statement newThrower;
	private final CFG origin;

	/**
	 * Builds the event.
	 * 
	 * @param state      the analysis state before the transfer
	 * @param result     the analysis state after the transfer
	 * @param newThrower the statement to use as new thrower
	 * @param origin     the cfg from which throwers are being transferred
	 */
	public AnalysisTransferThrowersEnd(
			AnalysisState<A> state,
			AnalysisState<A> result,
			Statement newThrower,
			CFG origin) {
		this.state = state;
		this.result = result;
		this.newThrower = newThrower;
		this.origin = origin;
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

	/**
	 * Yields the statement to use as new thrower.
	 * 
	 * @return the statement
	 */
	public Statement getNewThrower() {
		return newThrower;
	}

	/**
	 * Yields the cfg from which throwers are being transferred.
	 * 
	 * @return the cfg
	 */
	public CFG getOrigin() {
		return origin;
	}

	@Override
	public String getTarget() {
		return "Analysis: Transferring throwers from " + origin + " to " + newThrower;
	}

}
