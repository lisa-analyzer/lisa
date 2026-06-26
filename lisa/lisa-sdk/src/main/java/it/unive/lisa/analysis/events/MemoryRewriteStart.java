package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.memory.MemoryLattice;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the start of the rewrite of a symbolic expression by the
 * memory domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <M> the type of {@link MemoryLattice} produced by the domain
 */
public class MemoryRewriteStart<M extends MemoryLattice<M>>
		extends
		Event
		implements
		DomainEvent,
		StartEvent {

	private final Class<?> domain;
	private final M state;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param domain     the domain class where the assignment happened
	 * @param state      the state before the computation
	 * @param expression the symbolic expression being tested
	 */
	public MemoryRewriteStart(
			Class<?> domain,
			M state,
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
	public M getState() {
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
