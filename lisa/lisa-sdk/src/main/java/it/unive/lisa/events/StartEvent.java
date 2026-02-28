package it.unive.lisa.events;

/**
 * A marker interface for events that signal the start of a specific phase.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface StartEvent {

	/**
	 * Yields a textual representation of the target of this start event.
	 * 
	 * @return the target
	 */
	String getTarget();
}
