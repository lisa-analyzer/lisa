package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.EvaluationEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the end of a semantics computation of a symbolic
 * expression by a domain taking part in the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link Lattice} produced by the domain
 */
public class DomainSmallStepEnd<L extends Lattice<L>>
		extends
		Event
		implements
		DomainEvent,
		EndEvent,
		EvaluationEvent<L, L> {

	private final Class<?> domain;
	private final ProgramPoint pp;
	private final L state;
	private final L result;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param domain     the domain class where the assignment happened
	 * @param pp         the program point where the computation happens
	 * @param state      the state before the computation
	 * @param result     the state after the computation
	 * @param expression the symbolic expression whose semantics has been
	 *                       computed
	 */
	public DomainSmallStepEnd(
			Class<?> domain,
			ProgramPoint pp,
			L state,
			L result,
			SymbolicExpression expression) {
		this.domain = domain;
		this.pp = pp;
		this.state = state;
		this.result = result;
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
	 * Yields the symbolic expression whose seamantics has been computed.
	 * 
	 * @return the symbolic expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
	}

	@Override
	public String getTarget() {
		return domain.getSimpleName() + ": Small step semantics of " + expression;
	}

}
