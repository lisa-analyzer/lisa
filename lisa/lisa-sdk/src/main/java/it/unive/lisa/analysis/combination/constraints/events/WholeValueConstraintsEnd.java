package it.unive.lisa.analysis.combination.constraints.events;

import it.unive.lisa.analysis.combination.constraints.WholeValue;
import it.unive.lisa.analysis.events.DomainEvent;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Set;

/**
 * An event signaling the end of the computation of the constraints by the
 * {@link it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class WholeValueConstraintsEnd
		extends
		Event
		implements
		DomainEvent,
		EndEvent {

	private final WholeValue state;
	private final Set<BinaryExpression> result;
	private final ValueExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param state      the state before the computation
	 * @param expression the symbolic expression being evaluated
	 * @param result     the generated constraints
	 */
	public WholeValueConstraintsEnd(
			WholeValue state,
			ValueExpression expression,
			Set<BinaryExpression> result) {
		this.state = state;
		this.result = result;
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

	/**
	 * Yields the generated constraints.
	 * 
	 * @return the generated constraints
	 */
	public Set<BinaryExpression> getResult() {
		return result;
	}

	@Override
	public String getTarget() {
		return "WholeValueAnalysis: Constraints of " + expression;
	}

}
