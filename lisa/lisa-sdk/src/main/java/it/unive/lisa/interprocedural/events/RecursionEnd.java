package it.unive.lisa.interprocedural.events;

import it.unive.lisa.events.EndEvent;
import it.unive.lisa.events.Event;

/**
 * An event signaling the end of the recursion solving during an interprocedural
 * analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RecursionEnd
		extends
		Event
		implements
		InterproceduralEvent,
		EndEvent {

	/**
	 * Builds the event.
	 */
	public RecursionEnd() {
		super();
	}

	@Override
	public String getTarget() {
		return "Recursion solving";
	}
}
