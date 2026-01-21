package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.EvaluationEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An event signaling the end of an assignment of a value to a symbolic
 * expression by a domain taking part in the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link Lattice} produced by the domain
 */
public class DomainAssignEnd<L extends Lattice<L>>
		extends
		Event
		implements
		DomainEvent,
		EndEvent,
		EvaluationEvent<L, L> {

	private final ProgramPoint pp;
	private final Class<?> domain;
	private final L state;
	private final L result;
	private final Identifier assigned;
	private final SymbolicExpression value;

	/**
	 * Builds the event.
	 * 
	 * @param pp       the program point where the computation happens
	 * @param domain   the domain class where the assignment happened
	 * @param state    the state before the assignment
	 * @param result   the state after the assignment
	 * @param assigned the symbolic expression being assigned to
	 * @param value    the value being assigned
	 */
	public DomainAssignEnd(
			Class<?> domain,
			ProgramPoint pp,
			L state,
			L result,
			Identifier assigned,
			SymbolicExpression value) {
		this.domain = domain;
		this.pp = pp;
		this.state = state;
		this.result = result;
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

	@Override
	public ProgramPoint getProgramPoint() {
		return pp;
	}

	@Override
	public L getPreState() {
		return state;
	}

	@Override
	public L getPostState() {
		return result;
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
