package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the start of an assignment of a value to a symbolic
 * expression during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisAssignStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		StartEvent {

	private final AnalysisState<A> state;
	private final SymbolicExpression assigned;
	private final SymbolicExpression value;

	/**
	 * Builds the event.
	 * 
	 * @param state    the analysis state before the assignment
	 * @param assigned the symbolic expression being assigned to
	 * @param value    the value being assigned
	 */
	public AnalysisAssignStart(
			AnalysisState<A> state,
			SymbolicExpression assigned,
			SymbolicExpression value) {
		this.state = state;
		this.assigned = assigned;
		this.value = value;
	}

	/**
	 * Yields the analysis state before the assignment.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	/**
	 * Yields the symbolic expression being assigned to.
	 * 
	 * @return the symbolic expression
	 */
	public SymbolicExpression getAssigned() {
		return assigned;
	}

	/**
	 * Yields the value being assigned.
	 * 
	 * @return the value
	 */
	public SymbolicExpression getValue() {
		return value;
	}

	@Override
	public String getTarget() {
		return "Analysis: Assignment of " + assigned + " to " + value;
	}

}
