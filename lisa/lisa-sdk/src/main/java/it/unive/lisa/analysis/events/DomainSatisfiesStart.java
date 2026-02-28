package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the start of a satisfiability test of a symbolic
 * expression by a domain taking part in the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link Lattice} produced by the domain
 */
public class DomainSatisfiesStart<L extends Lattice<L>>
		extends
		Event
		implements
		DomainEvent,
		StartEvent {

	private final Class<?> domain;
	private final L state;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param domain     the domain class where the assignment happened
	 * @param state      the state before the computation
	 * @param expression the symbolic expression being tested
	 */
	public DomainSatisfiesStart(
			Class<?> domain,
			L state,
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
	public L getState() {
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
		return domain.getSimpleName() + ": Satisfies of " + expression;
	}

}
