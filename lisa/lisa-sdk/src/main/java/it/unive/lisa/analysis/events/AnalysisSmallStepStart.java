package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the start of a semantics computation of a symbolic
 * expression during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisSmallStepStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		StartEvent {

	private final AnalysisState<A> state;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param state      the analysis state before the computation
	 * @param expression the symbolic expression whose semantics is been
	 *                       computed
	 */
	public AnalysisSmallStepStart(
			AnalysisState<A> state,
			SymbolicExpression expression) {
		this.state = state;
		this.expression = expression;
	}

	/**
	 * Yields the analysis state before the computation.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	/**
	 * Yields the symbolic expression whose semantics is being computed.
	 * 
	 * @return the symbolic expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
	}

	@Override
	public String getTarget() {
		return "Analysis: Small step semantics of " + expression;
	}

}
