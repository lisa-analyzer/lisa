package it.unive.lisa.events;

/**
 * Common interface for listeners that can be registered to an
 * {@link EventQueue} to receive events.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface EventListener {

	/**
	 * Accepts and processes the given event. If this listener does not
	 * suppoport the given event, it can simply return.
	 * 
	 * @param event the event to process
	 */
	void onEvent(
			Event event);

	/**
	 * Callback invoked by {@link EventQueue} when {@link #onEvent(Event)}
	 * throws an exception.
	 * 
	 * @param event the event whose processing caused the error
	 * @param error the error thrown during processing
	 */
	default void onError(
			Event event,
			Exception error) {
	}
}
