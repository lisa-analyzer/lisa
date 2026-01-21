package it.unive.lisa.events;

/**
 * A marker interface for events that signal the end of a specific phase.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface EndEvent {

	/**
	 * Yields a textual representation of the target of this end event.
	 * 
	 * @return the target
	 */
	String getTarget();
}
