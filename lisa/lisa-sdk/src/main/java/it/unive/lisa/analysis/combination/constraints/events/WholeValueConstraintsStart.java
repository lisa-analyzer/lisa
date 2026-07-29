package it.unive.lisa.analysis.combination.constraints.events;

import it.unive.lisa.analysis.combination.constraints.WholeValue;
import it.unive.lisa.analysis.events.DomainEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * An event signaling the start of the computation of the constraints by the
 * {@link it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class WholeValueConstraintsStart
		extends
		Event
		implements
		DomainEvent,
		StartEvent {

	private final WholeValue state;
	private final ValueExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param state      the state before the computation
	 * @param expression the symbolic expression being evaluated
	 */
	public WholeValueConstraintsStart(
			WholeValue state,
			ValueExpression expression) {
		this.state = state;
		this.expression = expression;
	}

	/**
	 * Yields the state before the computation.
	 * 
	 * @return the state
	 */
	public WholeValue getState() {
		return state;
	}

	/**
	 * Yields the symbolic expression being evaluated.
	 * 
	 * @return the symbolic expression
	 */
	public ValueExpression getExpression() {
		return expression;
	}

	@Override
	public String getTarget() {
		return "WholeValueAnalysis: Constraints of " + expression;
	}

}
