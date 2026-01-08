package it.unive.lisa.events;

/**
 * Common interface for events generated during an analysis that can be posted
 * to an {@link EventQueue}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Event {

	private long timestamp;

	/**
	 * Builds the event, assigning it the current timestamp in nanoseconds.
	 */
	protected Event() {
		this.timestamp = System.nanoTime();
	}

	/**
	 * Yields the timestamp of this event, in nanoseconds.
	 * 
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
}
