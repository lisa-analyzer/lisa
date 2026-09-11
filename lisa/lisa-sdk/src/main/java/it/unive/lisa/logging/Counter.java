package it.unive.lisa.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * A counter that logs to a given logger while progressing during the count.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Counter {

	private static final Marker PROGRESS = MarkerManager.getMarker("PROGRESS");

	private static final int BAR_WIDTH = 50;

	/**
	 * Whether or not this counter is logging
	 */
	private boolean logging;

	/**
	 * The current count
	 */
	private int count;

	/**
	 * The last logged ratio
	 */
	private int ratio;

	/**
	 * The cap of the count, meaning the expected maximum number
	 */
	private final int cap;

	/**
	 * The message to display while counting
	 */
	private final String message;

	/**
	 * The objects being counted
	 */
	private final String objects;

	/**
	 * The logger to log onto
	 */
	private final Logger logger;

	/**
	 * The level to log at
	 */
	private final Level level;

	/**
	 * Logging start time
	 */
	private long startTime;

	/**
	 * Builds the counter. If {@code cap} is less than {@code 1} or
	 * {@code updateFactor} is negative, each {@link #count()} call will cause a
	 * new message to be logged. Otherwise, a message will be logged every
	 * {@code max(1, cap * updateFactor)} {@link #count()} call.
	 * 
	 * @param logger       the logger to log onto
	 * @param level        the level to log at
	 * @param message      the message to display while counting
	 * @param objects      the objects being counted
	 * @param cap          the cap of the count, meaning the expected maximum
	 *                         number
	 */
	public Counter(
			Logger logger,
			Level level,
			String message,
			String objects,
			int cap) {
		this.level = level;
		this.logging = false;
		this.count = 0;
		this.cap = cap;
		this.logger = logger;
		this.message = message;
		this.objects = objects;
	}

	/**
	 * Yields the current count of this counter.
	 * 
	 * @return the current count
	 */
	public int getCurrentCount() {
		return count;
	}

	/**
	 * Turns on the counter, logging the event.
	 * 
	 * @throws IllegalStateException if the counter is already logging
	 */
	public void on() {
		if (logging)
			throw new IllegalStateException("This counter is already logging");
		logging = true;
		startTime = System.nanoTime();
		logger.log(level, "{} [start]", message);
	}

	/**
	 * Yields {@code true} if this counter is currently logging updates.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isLogging() {
		return logging;
	}

	/**
	 * Progresses the count by one.
	 */
	public synchronized void count() {
		count++;
		if (logging)
			step();
	}

	/**
	 * Turns off the counter, logging the event.
	 */
	public void off() {
		if (!logging)
			return;

		logging = false;
		logger.log(
				level,
				"{} [stop] [{} {} in {}]",
				message,
				count,
				objects,
				TimeFormat.UP_TO_SECONDS.format(System.nanoTime() - startTime));
	}

	private void step() {
		double ratio = (double) count / cap;
		if (cap > 0 && ratio - this.ratio <= 0.01)
			return;

		String msg = message + ": ";
		if (cap > 0) {
			int filled = (int) (ratio * BAR_WIDTH);
			StringBuilder bar = new StringBuilder("[")
				.append("#".repeat(filled))
				.append("-".repeat(BAR_WIDTH - filled))
				.append("] ")
				.append((int) (ratio * 100))
				.append("%");
			msg += bar.toString();
		} else
			msg += "in progress (" + count + ")";

		logger.log(level, PROGRESS, msg);
	}

}
