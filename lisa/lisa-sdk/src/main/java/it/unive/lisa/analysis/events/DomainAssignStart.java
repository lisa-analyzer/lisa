package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An event signaling the start of an assignment of a value to a symbolic
 * expression by a domain taking part in the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link Lattice} produced by the domain
 */
public class DomainAssignStart<L extends Lattice<L>>
		extends
		Event
		implements
		DomainEvent,
		StartEvent {

	private final Class<?> domain;
	private final L state;
	private final Identifier assigned;
	private final SymbolicExpression value;

	/**
	 * Builds the event.
	 * 
	 * @param domain   the domain class where the assignment happened
	 * @param state    the state before the assignment
	 * @param assigned the symbolic expression being assigned to
	 * @param value    the value being assigned
	 */
	public DomainAssignStart(
			Class<?> domain,
			L state,
			Identifier assigned,
			SymbolicExpression value) {
		this.domain = domain;
		this.state = state;
		this.assigned = assigned;
		this.value = value;
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
	 * Yields the state before the assignment.
	 * 
	 * @return the state
	 */
	public L getState() {
		return state;
	}

	/**
	 * Yields the symbolic expression being assigned to.
	 * 
	 * @return the symbolic expression
	 */
	public Identifier getAssigned() {
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
		return domain.getSimpleName() + ": Assignment of " + assigned + " to " + value;
	}

}
