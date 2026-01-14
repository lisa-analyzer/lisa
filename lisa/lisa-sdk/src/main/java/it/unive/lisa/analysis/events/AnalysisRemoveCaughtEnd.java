package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An event signaling the end of the removal of caught errors during the
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisRemoveCaughtEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent {

	private final AnalysisState<A> state;
	private final AnalysisState<A> result;
	private final Statement where;

	/**
	 * Builds the event.
	 * 
	 * @param state  the analysis state before the removal
	 * @param result the analysis state after the removal
	 * @param where  the statement where caught errors are being removed
	 */
	public AnalysisRemoveCaughtEnd(
			AnalysisState<A> state,
			AnalysisState<A> result,
			Statement where) {
		this.state = state;
		this.result = result;
		this.where = where;
	}

	/**
	 * Yields the analysis state before the removal.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	/**
	 * Yields the analysis state after the removal.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getResult() {
		return result;
	}

	/**
	 * Yields the statement where caught errors are being removed.
	 * 
	 * @return the statement
	 */
	public Statement getWhere() {
		return where;
	}

	@Override
	public String getTarget() {
		return "Analysis: Removing caught errors at " + where;
	}

}
