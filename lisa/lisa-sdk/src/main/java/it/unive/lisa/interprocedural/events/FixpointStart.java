package it.unive.lisa.interprocedural.events;

import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;

/**
 * An event signaling the start of program-wide fixpoint of the interprocedural
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FixpointStart
		extends
		Event
		implements
		InterproceduralEvent,
		StartEvent {

	/**
	 * Builds the event.
	 */
	public FixpointStart() {
		super();
	}

	@Override
	public String getTarget() {
		return "Program-wide fixpoint";
	}

}
