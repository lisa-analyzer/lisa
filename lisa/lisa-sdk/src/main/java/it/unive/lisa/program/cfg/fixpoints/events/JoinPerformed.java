package it.unive.lisa.program.cfg.fixpoints.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An event signaling that the join operation between subsequent abstractions
 * for the same Statement has been performed during a fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class JoinPerformed<A extends AbstractLattice<A>>
		extends
		Event
		implements
		FixpointEvent {

	private final Statement statement;
	private final CompoundState<A> prev;
	private final CompoundState<A> current;
	private final CompoundState<A> result;

	/**
	 * Builds the event.
	 * 
	 * @param statement the statement whose abstractions have been compared
	 * @param prev      the previous abstraction
	 * @param current   the current abstraction
	 * @param result    the result of the comparison
	 */
	public JoinPerformed(
			Statement statement,
			CompoundState<A> prev,
			CompoundState<A> current,
			CompoundState<A> result) {
		this.statement = statement;
		this.prev = prev;
		this.current = current;
		this.result = result;
	}

	/**
	 * Yields the statement whose abstractions have been compared.
	 * 
	 * @return the statement
	 */
	public Statement getStatement() {
		return statement;
	}

	/**
	 * Yields the previous abstraction.
	 * 
	 * @return the previous abstraction
	 */
	public CompoundState<A> getPrev() {
		return prev;
	}

	/**
	 * Yields the current abstraction.
	 * 
	 * @return the current abstraction
	 */
	public CompoundState<A> getCurrent() {
		return current;
	}

	/**
	 * Yields the result of the comparison.
	 * 
	 * @return the result
	 */
	public CompoundState<A> getResult() {
		return result;
	}

}
