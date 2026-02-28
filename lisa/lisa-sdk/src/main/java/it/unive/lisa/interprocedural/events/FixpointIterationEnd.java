package it.unive.lisa.interprocedural.events;

import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;

/**
 * An event signaling the end of a fixpoint iteration during an interprocedural
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FixpointIterationEnd
		extends
		Event
		implements
		InterproceduralEvent,
		EndEvent {

	private final int iteration;

	/**
	 * Builds the event.
	 * 
	 * @param iteration the current iteration number
	 */
	public FixpointIterationEnd(
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
