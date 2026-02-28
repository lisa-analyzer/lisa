package it.unive.lisa.program.cfg.fixpoints.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An event signaling the end of the semantics computation for a given Statement
 * during a fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class StatementSemanticsEnd<A extends AbstractLattice<A>>
		extends
		Event
		implements
		FixpointEvent,
		EndEvent {

	private final Statement statement;
	private final AnalysisState<A> entryState;
	private final AnalysisState<A> result;

	/**
	 * Builds the event.
	 * 
	 * @param statement  the statement whose semantics is being computed
	 * @param entryState the entry state for the fixpoint
	 * @param result     the result state of the semantics computation
	 */
	public StatementSemanticsEnd(
			Statement statement,
			AnalysisState<A> entryState,
			AnalysisState<A> result) {
		this.statement = statement;
		this.entryState = entryState;
		this.result = result;
	}

	/**
	 * Yields the statement whose semantics is being computed.
	 * 
	 * @return the statement
	 */
	public Statement getStatement() {
		return statement;
	}

	/**
	 * Yields the entry state for the fixpoint.
	 * 
	 * @return the entry state
	 */
	public AnalysisState<A> getEntryState() {
		return entryState;
	}

	/**
	 * Yields the result state of the semantics computation.
	 * 
	 * @return the result state
	 */
	public AnalysisState<A> getResult() {
		return result;
	}

	@Override
	public String getTarget() {
		return "Semantics of " + statement;
	}
}
