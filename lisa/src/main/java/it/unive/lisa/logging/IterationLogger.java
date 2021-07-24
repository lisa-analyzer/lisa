package it.unive.lisa.logging;

import it.unive.lisa.util.collections.IterableArray;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * An utility class that allows automatic logging while iterating over elements
 * of a collection, stream or array.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class IterationLogger {

	private IterationLogger() {
		// this class is just a static holder
	}

	/**
	 * Wraps the given array into an {@link Iterable} instance that
	 * automatically logs at level {@link Level#INFO} while traversing the
	 * array.
	 * 
	 * @param <E>     the type of elements in the array
	 * @param logger  the logger to log onto
	 * @param array   the array to be iterated over
	 * @param message the message to display at each update
	 * @param objects the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the array
	 */
	public static <E> Iterable<E> iterate(Logger logger, E[] array, String message, String objects) {
		return iterate(logger, Level.INFO, new IterableArray<>(array), message, objects, array.length);
	}

	/**
	 * Wraps the given array into an {@link Iterable} instance that
	 * automatically logs while traversing the array.
	 * 
	 * @param <E>     the type of elements in the array
	 * @param logger  the logger to log onto
	 * @param level   the {@link Level} to log to
	 * @param array   the array to be iterated over
	 * @param message the message to display at each update
	 * @param objects the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the array
	 */
	public static <E> Iterable<E> iterate(Logger logger, Level level, E[] array, String message, String objects) {
		return iterate(logger, level, new IterableArray<>(array), message, objects, array.length);
	}

	/**
	 * Wraps the given collection into an {@link Iterable} instance that
	 * automatically logs at level {@link Level#INFO} while traversing the
	 * collection.
	 * 
	 * @param <E>        the type of elements in the collection
	 * @param logger     the logger to log onto
	 * @param collection the collection to be iterated over
	 * @param message    the message to display at each update
	 * @param objects    the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the
	 *             collection
	 */
	public static <E> Iterable<E> iterate(Logger logger, Collection<E> collection, String message, String objects) {
		return iterate(logger, Level.INFO, collection, message, objects, collection.size());
	}

	/**
	 * Wraps the given collection into an {@link Iterable} instance that
	 * automatically logs while traversing the collection.
	 * 
	 * @param <E>        the type of elements in the collection
	 * @param logger     the logger to log onto
	 * @param level      the {@link Level} to log to
	 * @param collection the collection to be iterated over
	 * @param message    the message to display at each update
	 * @param objects    the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the
	 *             collection
	 */
	public static <E> Iterable<E> iterate(Logger logger, Level level, Collection<E> collection, String message,
			String objects) {
		return iterate(logger, level, collection, message, objects, collection.size());
	}

	/**
	 * Wraps the given iterable into an {@link Iterable} instance that
	 * automatically logs at level {@link Level#INFO} while traversing the
	 * array.
	 * 
	 * @param <E>      the type of elements in the iterable
	 * @param logger   the logger to log onto
	 * @param iterable the iterable to be iterated over
	 * @param message  the message to display at each update
	 * @param objects  the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the iterable
	 */
	public static <E> Iterable<E> iterate(Logger logger, Iterable<E> iterable, String message, String objects) {
		return iterate(logger, Level.INFO, iterable, message, objects);
	}

	/**
	 * Wraps the given iterable into an {@link Iterable} instance that
	 * automatically logs while traversing the iterable.
	 * 
	 * @param <E>      the type of elements in the iterable
	 * @param logger   the logger to log onto
	 * @param level    the {@link Level} to log to
	 * @param iterable the iterable to be iterated over
	 * @param message  the message to display at each update
	 * @param objects  the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the iterable
	 */
	public static <E> Iterable<E> iterate(Logger logger, Level level, Iterable<E> iterable, String message,
			String objects) {
		int size;
		Iterator<E> it;
		for (size = 0, it = iterable.iterator(); it.hasNext(); it.next(), size++)
			;
		return iterate(logger, level, iterable, message, objects, size);
	}

	/**
	 * Wraps the given stream into an {@link Iterable} instance that
	 * automatically logs at level {@link Level#INFO} while traversing a
	 * collection created from the stream.
	 * 
	 * @param <E>     the type of elements in the stream
	 * @param logger  the logger to log onto
	 * @param stream  the stream to be iterated over
	 * @param message the message to display at each update
	 * @param objects the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the
	 *             collection created from the stream
	 */
	public static <E> Iterable<E> iterate(Logger logger, Stream<E> stream, String message, String objects) {
		return iterate(logger, Level.INFO, stream, message, objects);
	}

	/**
	 * Wraps the given array into an {@link Iterable} instance that
	 * automatically logs while traversing a collection created from the stream.
	 * 
	 * @param <E>     the type of elements in the stream
	 * @param logger  the logger to log onto
	 * @param level   the {@link Level} to log to
	 * @param stream  the array to be iterated over
	 * @param message the message to display at each update
	 * @param objects the objects being counted
	 * 
	 * @return an iterable that automatically logs while traversing the
	 *             collection created from the stream
	 */
	public static <E> Iterable<E> iterate(Logger logger, Level level, Stream<E> stream, String message,
			String objects) {
		List<E> list = stream.collect(Collectors.toList());
		return iterate(logger, level, list, message, objects, list.size());
	}

	private static <E> Iterable<E> iterate(Logger logger, Level level, Iterable<E> it, String message, String objects,
			int size) {
		return new CountingIterable<>(it, new Counter(logger, level, message, objects, size, 0.025));
	}
}
