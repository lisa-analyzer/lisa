package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.EvaluationEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the end of an assignment of a value to a symbolic
 * expression during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisAssignEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent,
		EvaluationEvent<AnalysisState<A>, AnalysisState<A>> {

	private final ProgramPoint pp;
	private final AnalysisState<A> state;
	private final AnalysisState<A> result;
	private final SymbolicExpression assigned;
	private final SymbolicExpression value;

	/**
	 * Builds the event.
	 * 
	 * @param pp       the program point where the assignment happens
	 * @param state    the analysis state before the assignment
	 * @param result   the analysis state after the assignment
	 * @param assigned the symbolic expression being assigned to
	 * @param value    the value being assigned
	 */
	public AnalysisAssignEnd(
			ProgramPoint pp,
			AnalysisState<A> state,
			AnalysisState<A> result,
			SymbolicExpression assigned,
			SymbolicExpression value) {
		this.pp = pp;
		this.state = state;
		this.result = result;
		this.assigned = assigned;
		this.value = value;
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
