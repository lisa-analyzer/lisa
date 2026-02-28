package it.unive.lisa.interprocedural.events;

import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;

/**
 * An event signaling the start of a fixpoint iteration during an
 * interprocedural analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FixpointIterationStart
		extends
		Event
		implements
		InterproceduralEvent,
		StartEvent {

	private final int iteration;

	/**
	 * Builds the event.
	 * 
	 * @param iteration the current iteration number
	 */
	public FixpointIterationStart(
			int iteration) {
		super();
		this.iteration = iteration;
	}

	/**
	 * Yields the iteration number.
	 * 
	 * @return the iteration number
	 */
	public int getIteration() {
		return iteration;
	}

	@Override
	public String getTarget() {
		return "Program-wide fixpoint iteration " + iteration;
	}
}
