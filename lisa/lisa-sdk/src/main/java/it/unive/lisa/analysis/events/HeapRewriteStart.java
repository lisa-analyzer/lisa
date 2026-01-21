package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the start of the rewrite of a symbolic expression by the
 * heap domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the type of {@link HeapLattice} produced by the domain
 */
public class HeapRewriteStart<H extends HeapLattice<H>>
		extends
		Event
		implements
		DomainEvent,
		StartEvent {

	private final Class<?> domain;
	private final H state;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param domain     the domain class where the assignment happened
	 * @param state      the state before the computation
	 * @param expression the symbolic expression being tested
	 */
	public HeapRewriteStart(
			Class<?> domain,
			H state,
			SymbolicExpression expression) {
		this.domain = domain;
		this.state = state;
		this.expression = expression;
	}

	/**
	 * Yields the domain class where the assignment happened.
	 * 
	 * @return the domain class
	 */
	public Class<?> getDomain() {
		return domain;
	}

	/**
	 * Yields the state before the computation.
	 * 
	 * @return the state
	 */
	public H getState() {
		return state;
	}

	/**
	 * Yields the symbolic expression being assumed.
	 * 
	 * @return the symbolic expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
	}

	@Override
	public String getTarget() {
		return domain.getSimpleName() + ": Rewriting of " + expression;
	}

}
