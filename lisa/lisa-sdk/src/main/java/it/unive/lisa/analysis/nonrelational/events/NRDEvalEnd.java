package it.unive.lisa.analysis.nonrelational.events;

import it.unive.lisa.analysis.DomainLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.events.DomainEvent;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.lattices.FunctionalLattice;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An event signaling the end of the evaluation of a symbolic expression by a
 * non-relational domain taking part in the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link Lattice} used as value for {@code M}
 * @param <M> the type of {@link FunctionalLattice} produced by the domain
 */
public class NRDEvalEnd<
		L extends Lattice<L>,
		M extends FunctionalLattice<M, Identifier, L> & DomainLattice<M, M>>
		extends
		Event
		implements
		DomainEvent,
		EndEvent {

	private final Class<?> domain;
	private final M state;
	private final L result;
	private final SymbolicExpression expression;

	/**
	 * Builds the event.
	 * 
	 * @param domain     the domain class where the evaluation is happening
	 * @param state      the state before the computation
	 * @param result     the result of the evaluation
	 * @param expression the symbolic expression being evaluated
	 */
	public NRDEvalEnd(
			Class<?> domain,
			M state,
			L result,
			SymbolicExpression expression) {
		this.domain = domain;
		this.state = state;
		this.result = result;
		this.expression = expression;
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
	 * Yields the result of the evaluation.
	 * 
	 * @return the result
	 */
	public L getResult() {
		return result;
	}

	/**
	 * Yields the symbolic expression being evaluated.
	 * 
	 * @return the symbolic expression
	 */
	public SymbolicExpression getExpression() {
		return expression;
	}

	@Override
	public String getTarget() {
		return domain.getSimpleName() + ": Evaluation of " + expression;
	}

}
