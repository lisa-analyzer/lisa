package it.unive.lisa.analysis.events;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import java.util.Collection;

/**
 * An event signaling the start of the transfer of error states to the execution
 * state during the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} handled by the analysis
 */
public class AnalysisErrorsToExecStart<A extends AbstractLattice<A>>
		extends
		Event
		implements
		AnalysisEvent,
		StartEvent {

	private final AnalysisState<A> state;
	private final ProtectedBlock block;
	private final Collection<Type> targets;
	private final Collection<Type> excluded;
	private final VariableRef variable;

	/**
	 * Builds the event.
	 * 
	 * @param state    the analysis state before the transfer
	 * @param block    the protected block where error states are being moved
	 * @param targets  the types of errors to move
	 * @param excluded the types of errors to exclude from the move
	 * @param variable the (optional) variable where to move error states
	 */
	public AnalysisErrorsToExecStart(
			AnalysisState<A> state,
			ProtectedBlock block,
			Collection<Type> targets,
			Collection<Type> excluded,
			VariableRef variable) {
		this.state = state;
		this.block = block;
		this.targets = targets;
		this.excluded = excluded;
		this.variable = variable;
	}

	/**
	 * Yields the analysis state before the transfer.
	 * 
	 * @return the analysis state
	 */
	public AnalysisState<A> getState() {
		return state;
	}

	/**
	 * Yields the protected block where error states are being moved.
	 * 
	 * @return the protected block
	 */
	public ProtectedBlock getBlock() {
		return block;
	}

	/**
	 * Yields the types of errors to move.
	 * 
	 * @return the target types
	 */
	public Collection<Type> getTargets() {
		return targets;
	}

	/**
	 * Yields the types of errors to exclude from the move.
	 * 
	 * @return the excluded types
	 */
	public Collection<Type> getExcluded() {
		return excluded;
	}

	/**
	 * Yields the (optional) variable where to move error states.
	 * 
	 * @return the variable reference, or {@code null} if not specified
	 */
	public VariableRef getVariable() {
		return variable;
	}

	@Override
	public String getTarget() {
		return "Analysis: Moving error states to execution for block " + block;
	}

}
