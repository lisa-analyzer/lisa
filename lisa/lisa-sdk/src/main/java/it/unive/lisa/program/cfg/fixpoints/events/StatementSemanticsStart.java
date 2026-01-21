package it.unive.lisa.program.cfg.fixpoints.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An event signaling the start of the semantics computation for a given
 * Statement during a fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class StatementSemanticsStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		FixpointEvent,
		StartEvent {

	private final Statement statement;
	private final AnalysisState<A> entryState;

	/**
	 * Builds the event.
	 * 
	 * @param statement  the statement whose semantics is being computed
	 * @param entryState the entry state for the fixpoint
	 */
	public StatementSemanticsStart(
			Statement statement,
			AnalysisState<A> entryState) {
		this.statement = statement;
		this.entryState = entryState;
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

	@Override
	public String getTarget() {
		return "Semantics of " + statement;
	}
}
