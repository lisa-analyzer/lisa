package it.unive.lisa.logging;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * An utility class that allows automatic logging of the execution time of a
 * method.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TimerLogger {

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

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the return value
	 * @param <T0>     the type of the first argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0> R execFunction(Logger logger, String message, LoggableFunction<R, T0> function, T0 arg0) {
		return execFunction(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, function, arg0);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the return value
	 * @param <T0>     the type of the first argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0> R execFunction(Logger logger, Level logLevel, String message,
			LoggableFunction<R, T0> function, T0 arg0) {
		return execFunction(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, function, arg0);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>       the type of the returned value
	 * @param <T0>      the type of the first argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param function  the function to execute
	 * @param arg0      the first argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0> R execFunction(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableFunction<R, T0> function, T0 arg0) {
		Wrapper<R> w = new Wrapper<>();
		execAux(logger, logLevel, formatter, message, () -> w.ret = function.run(arg0));
		return w.ret;
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the return value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1> R execFunction(Logger logger, String message, LoggableBiFunction<R, T0, T1> function,
			T0 arg0, T1 arg1) {
		return execFunction(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the return value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1> R execFunction(Logger logger, Level logLevel, String message,
			LoggableBiFunction<R, T0, T1> function, T0 arg0, T1 arg1) {
		return execFunction(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>       the type of the returned value
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param function  the function to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1> R execFunction(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableBiFunction<R, T0, T1> function, T0 arg0, T1 arg1) {
		Wrapper<R> w = new Wrapper<>();
		execAux(logger, logLevel, formatter, message, () -> w.ret = function.run(arg0, arg1));
		return w.ret;
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2> R execFunction(Logger logger, String message,
			LoggableTriFunction<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2) {
		return execFunction(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1, arg2);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2> R execFunction(Logger logger, Level logLevel, String message,
			LoggableTriFunction<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2) {
		return execFunction(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1, arg2);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>       the type of the returned value
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param <T2>      the type of the third argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param function  the function to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 * @param arg2      the third argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2> R execFunction(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableTriFunction<R, T0, T1, T2> function, T0 arg0, T1 arg1, T2 arg2) {
		Wrapper<R> w = new Wrapper<>();
		execAux(logger, logLevel, formatter, message, () -> w.ret = function.run(arg0, arg1, arg2));
		return w.ret;
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2, T3> R execFunction(Logger logger, String message,
			LoggableQuadFunction<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3) {
		return execFunction(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1, arg2, arg3);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2, T3> R execFunction(Logger logger, Level logLevel, String message,
			LoggableQuadFunction<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3) {
		return execFunction(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1, arg2, arg3);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>       the type of the returned value
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param <T2>      the type of the third argument
	 * @param <T3>      the type of the fourth argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param function  the function to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 * @param arg2      the third argument
	 * @param arg3      the fourth argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2, T3> R execFunction(Logger logger, Level logLevel, TimeFormat formatter,
			String message, LoggableQuadFunction<R, T0, T1, T2, T3> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3) {
		Wrapper<R> w = new Wrapper<>();
		execAux(logger, logLevel, formatter, message, () -> w.ret = function.run(arg0, arg1, arg2, arg3));
		return w.ret;
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param <T4>     the type of the fifth argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 * @param arg4     the fifth argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2, T3, T4> R execFunction(Logger logger, String message,
			LoggablePentaFunction<R, T0, T1, T2, T3, T4> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
		return execFunction(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1, arg2, arg3,
				arg4);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param <T4>     the type of the fifth argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 * @param arg4     the fifth argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2, T3, T4> R execFunction(Logger logger, Level logLevel, String message,
			LoggablePentaFunction<R, T0, T1, T2, T3, T4> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
		return execFunction(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, function, arg0, arg1, arg2, arg3,
				arg4);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>       the type of the returned value
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param <T2>      the type of the third argument
	 * @param <T3>      the type of the fourth argument
	 * @param <T4>      the type of the fifth argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param function  the function to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 * @param arg2      the third argument
	 * @param arg3      the fourth argument
	 * @param arg4      the fifth argument
	 * 
	 * @return the returned value
	 */
	public static <R, T0, T1, T2, T3, T4> R execFunction(Logger logger, Level logLevel, TimeFormat formatter,
			String message, LoggablePentaFunction<R, T0, T1, T2, T3, T4> function, T0 arg0, T1 arg1, T2 arg2, T3 arg3,
			T4 arg4) {
		Wrapper<R> w = new Wrapper<>();
		execAux(logger, logLevel, formatter, message, () -> w.ret = function.run(arg0, arg1, arg2, arg3, arg4));
		return w.ret;
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param args     the arguments to be consumed
	 * 
	 * @return the returned value
	 */
	public static <R> R execFunction(Logger logger, String message, LoggableMultiFunction<R> function, Object... args) {
		return execFunction(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, function, args);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>      the type of the returned value
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param function the function to execute
	 * @param args     the arguments to be consumed
	 * 
	 * @return the returned value
	 */
	public static <R> R execFunction(Logger logger, Level logLevel, String message, LoggableMultiFunction<R> function,
			Object... args) {
		return execFunction(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, function, args);
	}

	/**
	 * Executes the given function, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <R>       the type of the returned value
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param function  the function to execute
	 * @param args      the arguments to be consumed
	 * 
	 * @return the returned value
	 */
	public static <R> R execFunction(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableMultiFunction<R> function, Object... args) {
		Wrapper<R> w = new Wrapper<>();
		execAux(logger, logLevel, formatter, message, () -> w.ret = function.run(args));
		return w.ret;
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param function the consumer to execute
	 * @param arg0     the first argument
	 */
	public static <T0> void execConsumer(Logger logger, String message, LoggableConsumer<T0> function, T0 arg0) {
		execConsumer(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, function, arg0);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 */
	public static <T0> void execConsumer(Logger logger, Level logLevel, String message, LoggableConsumer<T0> consumer,
			T0 arg0) {
		execConsumer(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, consumer, arg0);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>      the type of the first argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param consumer  the consumer to execute
	 * @param arg0      the first argument
	 */
	public static <T0> void execConsumer(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableConsumer<T0> consumer, T0 arg0) {
		execAux(logger, logLevel, formatter, message, () -> consumer.run(arg0));
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 */
	public static <T0, T1> void execConsumer(Logger logger, String message, LoggableBiConsumer<T0, T1> consumer,
			T0 arg0, T1 arg1) {
		execConsumer(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 */
	public static <T0, T1> void execConsumer(Logger logger, Level logLevel, String message,
			LoggableBiConsumer<T0, T1> consumer, T0 arg0, T1 arg1) {
		execConsumer(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param consumer  the consumer to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 */
	public static <T0, T1> void execConsumer(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableBiConsumer<T0, T1> consumer, T0 arg0, T1 arg1) {
		execAux(logger, logLevel, formatter, message, () -> consumer.run(arg0, arg1));
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 */
	public static <T0, T1, T2> void execConsumer(Logger logger, String message,
			LoggableTriConsumer<T0, T1, T2> consumer, T0 arg0, T1 arg1, T2 arg2) {
		execConsumer(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1, arg2);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 */
	public static <T0, T1, T2> void execConsumer(Logger logger, Level logLevel, String message,
			LoggableTriConsumer<T0, T1, T2> consumer, T0 arg0, T1 arg1, T2 arg2) {
		execConsumer(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1, arg2);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param <T2>      the type of the third argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param consumer  the consumer to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 * @param arg2      the third argument
	 */
	public static <T0, T1, T2> void execConsumer(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableTriConsumer<T0, T1, T2> consumer, T0 arg0, T1 arg1, T2 arg2) {
		execAux(logger, logLevel, formatter, message, () -> consumer.run(arg0, arg1, arg2));
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 */
	public static <T0, T1, T2, T3> void execConsumer(Logger logger, String message,
			LoggableQuadConsumer<T0, T1, T2, T3> consumer, T0 arg0, T1 arg1, T2 arg2, T3 arg3) {
		execConsumer(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1, arg2, arg3);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 */
	public static <T0, T1, T2, T3> void execConsumer(Logger logger, Level logLevel, String message,
			LoggableQuadConsumer<T0, T1, T2, T3> consumer, T0 arg0, T1 arg1, T2 arg2, T3 arg3) {
		execConsumer(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1, arg2, arg3);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param <T2>      the type of the third argument
	 * @param <T3>      the type of the fourth argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param consumer  the consumer to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 * @param arg2      the third argument
	 * @param arg3      the fourth argument
	 */
	public static <T0, T1, T2, T3> void execConsumer(Logger logger, Level logLevel, TimeFormat formatter,
			String message, LoggableQuadConsumer<T0, T1, T2, T3> consumer, T0 arg0, T1 arg1, T2 arg2, T3 arg3) {
		execAux(logger, logLevel, formatter, message, () -> consumer.run(arg0, arg1, arg2, arg3));
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param <T4>     the type of the fifth argument
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 * @param arg4     the fifth argument
	 */
	public static <T0, T1, T2, T3, T4> void execConsumer(Logger logger, String message,
			LoggablePentaConsumer<T0, T1, T2, T3, T4> consumer, T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
		execConsumer(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1, arg2, arg3, arg4);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>     the type of the first argument
	 * @param <T1>     the type of the second argument
	 * @param <T2>     the type of the third argument
	 * @param <T3>     the type of the fourth argument
	 * @param <T4>     the type of the fifth argument
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param arg0     the first argument
	 * @param arg1     the second argument
	 * @param arg2     the third argument
	 * @param arg3     the fourth argument
	 * @param arg4     the fifth argument
	 */
	public static <T0, T1, T2, T3, T4> void execConsumer(Logger logger, Level logLevel, String message,
			LoggablePentaConsumer<T0, T1, T2, T3, T4> consumer, T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
		execConsumer(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, consumer, arg0, arg1, arg2, arg3, arg4);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param <T0>      the type of the first argument
	 * @param <T1>      the type of the second argument
	 * @param <T2>      the type of the third argument
	 * @param <T3>      the type of the fourth argument
	 * @param <T4>      the type of the fifth argument
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param consumer  the consumer to execute
	 * @param arg0      the first argument
	 * @param arg1      the second argument
	 * @param arg2      the third argument
	 * @param arg3      the fourth argument
	 * @param arg4      the fifth argument
	 */
	public static <T0, T1, T2, T3, T4> void execConsumer(Logger logger, Level logLevel, TimeFormat formatter,
			String message, LoggablePentaConsumer<T0, T1, T2, T3, T4> consumer, T0 arg0, T1 arg1, T2 arg2, T3 arg3,
			T4 arg4) {
		execAux(logger, logLevel, formatter, message, () -> consumer.run(arg0, arg1, arg2, arg3, arg4));
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The log is issued at {@link Level#INFO} level, and the elapsed
	 * time is formatted up to minutes.
	 * 
	 * @param logger   the logger to log onto
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param args     the arguments to be consumed
	 */
	public static void execConsumer(Logger logger, String message, LoggableMultiConsumer consumer, Object... args) {
		execConsumer(logger, Level.INFO, TimeFormat.UP_TO_MINUTES, message, consumer, args);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param logger   the logger to log onto
	 * @param logLevel the level to log at
	 * @param message  the message to display
	 * @param consumer the consumer to execute
	 * @param args     the arguments to be consumed
	 */
	public static void execConsumer(Logger logger, Level logLevel, String message, LoggableMultiConsumer consumer,
			Object... args) {
		execConsumer(logger, logLevel, TimeFormat.UP_TO_MINUTES, message, consumer, args);
	}

	/**
	 * Executes the given consumer, while logging the time need for it to
	 * complete. The elapsed time is formatted up to minutes.
	 * 
	 * @param logger    the logger to log onto
	 * @param logLevel  the level to log at
	 * @param formatter the time format to log
	 * @param message   the message to display
	 * @param consumer  the consumer to execute
	 * @param args      the arguments to be consumed
	 */
	public static void execConsumer(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableMultiConsumer consumer, Object... args) {
		execAux(logger, logLevel, formatter, message, () -> consumer.run(args));
	}

	private static void execAux(Logger logger, Level logLevel, TimeFormat formatter, String message,
			LoggableAction action) {
		long startTime = System.nanoTime();
		logger.log(logLevel, message + " [start]");
		action.run();
		logger.log(logLevel,
				message + " [stop] [completed in " + formatter.format(System.nanoTime() - startTime) + "]");
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

	/**
	 * A {@link Function} consuming one argument and producing one value.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <R>  the type of the return value
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableFunction<R, T0> {

		/**
		 * Runs this function.
		 * 
		 * @param arg0 the first argument
		 * 
		 * @return the function result
		 */
		R run(T0 arg0);
	}

	/**
	 * A {@link Function} consuming two arguments and producing one value.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * @param <R>  the type of the return value
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableBiFunction<R, T0, T1> {

		/**
		 * Runs this function.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 * 
		 * @return the function result
		 */
		R run(T0 arg0, T1 arg1);
	}

	/**
	 * A {@link Function} consuming three arguments and producing one value.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * @param <T2> the type of the third argument
	 * @param <R>  the type of the return value
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableTriFunction<R, T0, T1, T2> {

		/**
		 * Runs this function.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 * @param arg2 the third argument
		 * 
		 * @return the function result
		 */
		R run(T0 arg0, T1 arg1, T2 arg2);
	}

	/**
	 * A {@link Function} consuming four arguments and producing one value.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * @param <T2> the type of the third argument
	 * @param <T3> the type of the fourth argument
	 * @param <R>  the type of the return value
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableQuadFunction<R, T0, T1, T2, T3> {

		/**
		 * Runs this function.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 * @param arg2 the third argument
		 * @param arg3 the fourth argument
		 * 
		 * @return the function result
		 */
		R run(T0 arg0, T1 arg1, T2 arg2, T3 arg3);
	}

	/**
	 * A {@link Function} consuming five arguments and producing one value.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * @param <T2> the type of the third argument
	 * @param <T3> the type of the fourth argument
	 * @param <T4> the type of the fifth argument
	 * @param <R>  the type of the return value
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggablePentaFunction<R, T0, T1, T2, T3, T4> {

		/**
		 * Runs this function.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 * @param arg2 the third argument
		 * @param arg3 the fourth argument
		 * @param arg4 the fifth argument
		 * 
		 * @return the function result
		 */
		R run(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4);
	}

	/**
	 * A {@link Function} consuming multiple arguments and producing one value.
	 * 
	 * @param <R> the type of the return value
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableMultiFunction<R> {
		/**
		 * Runs this function.
		 * 
		 * @param args the arguments to be consumed
		 * 
		 * @return the function result
		 */
		R run(Object... args);
	}

	/**
	 * A {@link Consumer} consuming one argument.
	 * 
	 * @param <T0> the type of the argument
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableConsumer<T0> {

		/**
		 * Runs this consumer.
		 * 
		 * @param arg0 the first argument
		 */
		void run(T0 arg0);
	}

	/**
	 * A {@link Consumer} consuming two arguments.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableBiConsumer<T0, T1> {

		/**
		 * Runs this consumer.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 */
		void run(T0 arg0, T1 arg1);
	}

	/**
	 * A {@link Consumer} consuming three arguments.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * @param <T2> the type of the third argument
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableTriConsumer<T0, T1, T2> {

		/**
		 * Runs this consumer.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 * @param arg2 the third argument
		 */
		void run(T0 arg0, T1 arg1, T2 arg2);
	}

	/**
	 * A {@link Consumer} consuming four arguments.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * @param <T2> the type of the third argument
	 * @param <T3> the type of the fourth argument
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableQuadConsumer<T0, T1, T2, T3> {

		/**
		 * Runs this consumer.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 * @param arg2 the third argument
		 * @param arg3 the fourth argument
		 */
		void run(T0 arg0, T1 arg1, T2 arg2, T3 arg3);
	}

	/**
	 * A {@link Consumer} consuming five arguments.
	 * 
	 * @param <T0> the type of the first argument
	 * @param <T1> the type of the second argument
	 * @param <T2> the type of the third argument
	 * @param <T3> the type of the fourth argument
	 * @param <T4> the type of the fifth argument
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggablePentaConsumer<T0, T1, T2, T3, T4> {

		/**
		 * Runs this consumer.
		 * 
		 * @param arg0 the first argument
		 * @param arg1 the second argument
		 * @param arg2 the third argument
		 * @param arg3 the fourth argument
		 * @param arg4 the fifth argument
		 */
		void run(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4);
	}

	/**
	 * A {@link Consumer} consuming multiple arguments.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	@FunctionalInterface
	public interface LoggableMultiConsumer {

		/**
		 * Runs this consumer.
		 * 
		 * @param args the arguments to be consumed
		 */
		void run(Object... args);
	}
}