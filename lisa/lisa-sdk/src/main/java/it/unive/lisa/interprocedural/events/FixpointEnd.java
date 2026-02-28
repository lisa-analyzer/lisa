package it.unive.lisa.interprocedural.events;

import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;

/**
 * An event signaling the end of program-wide fixpoint of the interprocedural
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FixpointEnd
		extends
		Event
		implements
		InterproceduralEvent,
		EndEvent {

	/**
	 * Builds the event.
	 */
	public FixpointEnd() {
		super();
	}

	@Override
	public String getTarget() {
		return "Program-wide fixpoint";
	}
}
