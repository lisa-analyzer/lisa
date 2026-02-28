package it.unive.lisa.interprocedural.events;

import it.unive.lisa.events.Event;
import it.unive.lisa.events.StartEvent;

/**
 * An event signaling the start of the recursion solving during an
 * interprocedural analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class RecursionStart
		extends
		Event
		implements
		InterproceduralEvent,
		StartEvent {

	/**
	 * Builds the event.
	 */
	public RecursionStart() {
		super();
	}

	@Override
	public String getTarget() {
		return "Recursion solving";
	}
}
