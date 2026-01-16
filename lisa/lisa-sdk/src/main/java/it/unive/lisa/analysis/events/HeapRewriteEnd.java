package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * An event signaling the end of the rewrite of a symbolic expression by the
 * heap domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the type of {@link HeapLattice} produced by the domain
 */
public class HeapRewriteEnd<H extends HeapLattice<H>>
		extends
		Event
		implements
		DomainEvent,
		EndEvent {

	private final Class<?> domain;
	private final H state;
	private final SymbolicExpression expression;
	private final ExpressionSet result;

	/**
	 * Builds the event.
	 * 
	 * @param domain     the domain class where the assignment happened
	 * @param state      the state before the computation
	 * @param expression the symbolic expression being tested
	 * @param result     the rewritten expressions
	 */
	public HeapRewriteEnd(
			Class<?> domain,
			H state,
			SymbolicExpression expression,
			ExpressionSet result) {
		this.domain = domain;
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

	/**
	 * Yields the state before the computation.
	 * 
	 * @return the state
	 */
	public H getState() {
		return state;
	}

	/**
	 * Yields the rewritten expressions.
	 * 
	 * @return the result
	 */
	public ExpressionSet getResult() {
		return result;
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
