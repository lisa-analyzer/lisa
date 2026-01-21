package it.unive.lisa.events;

import it.unive.lisa.ReportingTool;

/**
 * Common interface for listeners that can be registered to an
 * {@link EventQueue} to receive events.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface EventListener {

	/**
	 * Callback invoked only once before the beginning of the analysis. Can be
	 * used to setup common data structures. The default implementation does
	 * nothing.
	 * 
	 * @param tool the tool that this listener can use during the execution
	 */
	default void beforeExecution(
			ReportingTool tool) {
	}

	/**
	 * Callback invoked only once after the end of the analysis. Can be used to
	 * perform cleanups or to report summary warnings. The default
	 * implementation does nothing.
	 * 
	 * @param tool the tool that this listener can use during the execution
	 */
	default void afterExecution(
			ReportingTool tool) {
	}

	/**
	 * Accepts and processes the given event. If this listener does not
	 * suppoport the given event, it can simply return.
	 * 
	 * @param event the event to process
	 * @param tool  the tool that this listener can use during the execution
	 */
	void onEvent(
			Event event,
			ReportingTool tool);

	/**
	 * Callback invoked by {@link EventQueue} when
	 * {@link #onEvent(Event, ReportingTool)} throws an exception. The default
	 * implementation simply reports the error using the provided
	 * {@link ReportingTool} through a notice.
	 * 
	 * @param event the event whose processing caused the error
	 * @param error the error thrown during processing
	 * @param tool  the tool that this listener can use during the execution
	 */
	default void onError(
			Event event,
			Exception error,
			ReportingTool tool) {
		tool.notice(getClass().getSimpleName()
				+ " failed to process event of type "
				+ event.getClass().getSimpleName()
				+ ": " + error.getMessage());
	}
}
