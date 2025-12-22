package it.unive.lisa.events;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An event queue that allows posting {@link Event}s to registered synchronous
 * and asynchronous {@link EventListener}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class EventQueue
		implements
		AutoCloseable {

	private final EventListener[] syncListeners;
	private final EventListener[] asyncListeners;

	private final BlockingQueue<Event> asyncQueue;
	private final Thread asyncThread;
	private final boolean hasListeners;

	private volatile boolean running = true;

	/**
	 * Builds the event queue.
	 * 
	 * @param syncListeners  the synchronous listeners
	 * @param asyncListeners the asynchronous listeners
	 */
	public EventQueue(
			List<EventListener> syncListeners,
			List<EventListener> asyncListeners) {
		this.syncListeners = syncListeners.toArray(new EventListener[0]);
		this.asyncListeners = asyncListeners.toArray(new EventListener[0]);
		this.hasListeners = !syncListeners.isEmpty() || !asyncListeners.isEmpty();

		this.asyncQueue = new LinkedBlockingQueue<>();

		this.asyncThread = new Thread(this::asyncLoop, "event-async-dispatcher");
		this.asyncThread.setDaemon(true);
		this.asyncThread.start();
	}

	/**
	 * Posts the given event to all registered listeners.
	 * 
	 * @param event the event to post
	 */
	public void post(
			Event event) {
		if (!hasListeners)
			return;

		// synchronous listeners (hot path)
		for (EventListener l : syncListeners)
			try {
				l.onEvent(event);
			} catch (Exception e) {
				l.onError(event, e);
			}

		// asynchronous listeners (enqueue once)
		if (asyncListeners.length > 0)
			asyncQueue.offer(event);
	}

	/**
	 * Waits for all previously posted asynchronous events to be processed.
	 * 
	 * @throws InterruptedException if the current thread is interrupted while
	 *                                  waiting
	 */
	public void join() throws InterruptedException {
		if (asyncListeners.length == 0)
			// nothing async to wait for
			return;

		DrainEvent drain = new DrainEvent();
		asyncQueue.offer(drain);
		drain.latch.await();
	}

	private void asyncLoop() {
		try {
			while (running) {
				Event event = asyncQueue.take();

				if (event instanceof DrainEvent) {
					// since async listeners run sequentially, the drain event
					// being processed ensures that all prior events have been
					// processed
					((DrainEvent) event).latch.countDown();
					continue;
				}

				for (EventListener l : asyncListeners)
					try {
						l.onEvent(event);
					} catch (Exception e) {
						l.onError(event, e);
					}
			}
		} catch (InterruptedException ignored) {
			// shutdown
		}
	}

	@Override
	public void close() throws InterruptedException {
		join();
		running = false;
		asyncThread.interrupt();
		asyncThread.join();
	}

	private static class DrainEvent
			implements
			Event {
		final CountDownLatch latch = new CountDownLatch(1);
	}

}
