package it.unive.lisa.logging;

import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * An utility class that allows automatic logging of the execution time of a
 * method.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class TimerLogger {

	private TimerLogger() {
		// this class is just a static holder
	}

	/**
	 * Executes the given action, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param logger  the logger to log onto
	 * @param message the message to display
	 * @param action  the action to execute
	 */
	public static void execAction(Logger logger, String message, LoggableAction action) {
		execAction(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, action);
	}

	/**
	 * Executes the given action, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param action   the action to execute
	 */
	public static void execAction(Logger logger, Level logLevel, String message, LoggableAction action) {
		execAction(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, action);
	}

	/**
	 * Executes the given action, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param action    the action to execute
	 */
	public static void execAction(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableAction action) {
		execAux(logger, logLevel, formatter, message, action);
	}

	/**
	 * Executes the given supplier, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <T>      the type of the supplied value
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param supplier the supplier to execute
	 * 
	 * @return the supplied value
	 */
	public static <T> T execSupplier(Logger logger, String message, LoggableSupplier<T> supplier) {
		return execSupplier(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, supplier);
	}

	/**
	 * Executes the given supplier, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T>      the type of the supplied value
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param supplier the supplier to execute
	 * 
	 * @return the supplied value
	 */
	public static <T> T execSupplier(Logger logger, Level logLevel, String message, LoggableSupplier<T> supplier) {
		return execSupplier(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, supplier);
	}

	/**
	 * Executes the given supplier, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T>       the type of the supplied value
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param supplier  the supplier to execute
	 * 
	 * @return the supplied value
	 */
	public static <T> T execSupplier(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableSupplier<T> supplier) {
		Wrapper<T> w = new Wrapper<>();
		execAux(logger, logLevel, formatter, message, () -> w.ret = supplier.run());
		return w.ret;
	}

	private static void execAux(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableAction action) {
		long startTime = System.nanoTime();
		logger.log(logLevel, "{} [start]", message);
		action.run();
		logger.log(logLevel, "{} [stop] [completed in {}]", message, formatter.format(System.nanoTime() - startTime));
	}

	private static class Wrapper<T> {
		private T ret;
	}

	/**
	 * A {@link Runnable} executing a function with no arguments a no return
	 * value.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableAction {

		/**
		 * Runs the action.
		 */
		void run();
	}

	/**
	 * A {@link Supplier} producing one element.
	 * 
	 * @param <R> the type of the supplied element
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableSupplier<R> {

		/**
		 * Runs the supplier.
		 * 
		 * @return the produced element
		 */
		R run();
	}
}
