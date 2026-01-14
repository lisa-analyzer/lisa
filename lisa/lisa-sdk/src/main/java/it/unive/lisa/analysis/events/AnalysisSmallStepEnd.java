package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the end of a semantics computation of a symbolic
 * expression during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisSmallStepEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		EndEvent {

	private final AnalysisState<A> state;
	private final AnalysisState<A> result;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param state      the analysis state before the computation
	 * @param result     the analysis state after the computation
	 * @param expression the symbolic expression whose semantics has been
	 *                       computed
	 */
	public AnalysisSmallStepEnd(
			AnalysisState<A> state,
			AnalysisState<A> result,
			SymbolicExpression expression) {
		this.state = state;
		this.result = result;
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
	 * Yields the analysis state after the computation.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getResult() {
		return result;
	}

	/**
	 * Yields the symbolic expression whose seamantics has been computed.
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
