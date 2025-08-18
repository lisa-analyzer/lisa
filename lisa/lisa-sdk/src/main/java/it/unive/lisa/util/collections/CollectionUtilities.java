package it.unive.lisa.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Utility methods for operations on {@link Collection}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class CollectionUtilities {

	private CollectionUtilities() {
		// this class is just a static holder
	}

	/**
	 * A {@code null}-safe comparison callback that invokes the given comparator
	 * only if the given objects are both non-{@code null}.
	 * 
	 * @param <T>        the type of compared objects
	 * @param nullFirst  if {@code true}, {@code null} values will be treated as
	 *                       objects that appear before all non-{@code null}
	 *                       objects. This means that
	 *                       {@code nullSafeCompare(true, null, obj, ...)}
	 *                       returns {@code -1}. If {@code false}, the same
	 *                       invocation returns {@code 1}.
	 * @param left       the first element to compare
	 * @param right      the second element to compare
	 * @param comparator the comparator to use if both objects are
	 *                       non-{@code null}
	 * 
	 * @return an integer value determined by the ordering relation between
	 *             objects, as in {@link Comparator#compare(Object, Object)}
	 */
	public static <T> int nullSafeCompare(
			boolean nullFirst,
			T left,
			T right,
			Comparator<T> comparator) {
		if (left == null && right != null)
			return nullFirst ? -1 : 1;

		if (left != null && right == null)
			return nullFirst ? 1 : -1;

		if (left == null)
			return 0;

		return comparator.compare(left, right);
	}

	/**
	 * Tests if two collections contain the same elements, using a custom
	 * equality test to determine if two elements are to be considered equals.
	 * The two input collections are compared in an order-insensitive fashion.
	 * 
	 * @param <T>          the type of elements in the collections
	 * @param <C>          the type of the collections to test
	 * @param first        the first collection
	 * @param second       the second collection
	 * @param equalityTest the tester for equality of the objects within the
	 *                         collections
	 * 
	 * @return {@code true} only if the collections contain exactly the same set
	 *             of elements (order-insensitive)
	 */
	public static <T, C extends Collection<T>> boolean equals(
			C first,
			C second,
			BiPredicate<T, T> equalityTest) {
		// the following keeps track of the unmatched nodes in second
		Collection<T> copy = new HashSet<>(second);
		boolean found;
		for (T t1 : first) {
			found = false;
			for (T t2 : second)
				if (copy.contains(t2) && equalityTest.test(t1, t2)) {
					copy.remove(t2);
					found = true;
					break;
				}

			if (!found)
				// no match found
				return false;
		}

		// unmatched in other
		if (!copy.isEmpty())
			return false;

		return true;
	}

	/**
	 * Joins (union) two collections, using a custom equality test to determine
	 * if two elements are to be considered equals and thus joined together
	 * through the given {@code joiner}. All elements that, according to the
	 * given {@code equalityTest} are contained only in one of the input
	 * collections, will be added to the result as-is.
	 * 
	 * @param <T>          the type of elements in the collections
	 * @param <C>          the type of the collections to join
	 * @param first        the first collection
	 * @param second       the second collection
	 * @param result       the collection to be used as result, where joined
	 *                         elements will be added
	 * @param equalityTest the tester for equality of the objects within the
	 *                         collections
	 * @param joiner       the function that computes the join of two equal
	 *                         elements
	 */
	public static <T, C extends Collection<T>> void join(
			C first,
			C second,
			C result,
			BiPredicate<T, T> equalityTest,
			BinaryOperator<T> joiner) {
		// the following keeps track of the unmatched nodes in second
		Collection<T> copy = new HashSet<>(second);
		boolean found;
		for (T t1 : first) {
			found = false;
			for (T t2 : second)
				if (copy.contains(t2) && equalityTest.test(t1, t2)) {
					copy.remove(t2);
					found = true;
					result.add(joiner.apply(t1, t2));
					break;
				}

			if (!found)
				// no match found
				result.add(t1);
		}

		// unmatched in other
		copy.forEach(result::add);
	}

	/**
	 * Meets (intersection) two collections, using a custom equality test to
	 * determine if two elements are to be considered equals and thus met
	 * together through the given {@code meet}. All elements that, according to
	 * the given {@code equalityTest} are contained only in one of the input
	 * collections, will not be part of the result.
	 * 
	 * @param <T>          the type of elements in the collections
	 * @param <C>          the type of the collections to meet
	 * @param first        the first collection
	 * @param second       the second collection
	 * @param result       the collection to be used as result, where met
	 *                         elements will be added
	 * @param equalityTest the tester for equality of the objects within the
	 *                         collections
	 * @param meet         the function that computes the meet of two equal
	 *                         elements
	 */
	public static <T, C extends Collection<T>> void meet(
			C first,
			C second,
			C result,
			BiPredicate<T, T> equalityTest,
			BinaryOperator<T> meet) {
		// the following keeps track of the unmatched nodes in second
		Collection<T> copy = new HashSet<>(second);
		for (T t1 : first)
			for (T t2 : second)
				if (copy.contains(t2) && equalityTest.test(t1, t2)) {
					copy.remove(t2);
					result.add(meet.apply(t1, t2));
					break;
				}
	}

	/**
	 * Stores the given objects into a collection and returns it.
	 * 
	 * @param <T>  the type of objects
	 * @param objs the objects to store
	 * 
	 * @return a (modifiable) collection containing the given objects
	 */
	@SafeVarargs
	public static <T> Collection<T> collect(
			T... objs) {
		ArrayList<T> res = new ArrayList<>(objs.length);
		for (T o : objs)
			res.add(o);
		return res;
	}

	/**
	 * A {@link Collector} that yields a {@link SortedSet}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <E> the type of the elements to be sorted
	 */
	public static class SortedSetCollector<E> implements Collector<E, SortedSet<E>, SortedSet<E>> {

		@Override
		public Supplier<SortedSet<E>> supplier() {
			return () -> new TreeSet<>();
		}

		@Override
		public BiConsumer<SortedSet<E>, E> accumulator() {
			return (
					set,
					e
			) -> set.add(e);
		}

		@Override
		public BinaryOperator<SortedSet<E>> combiner() {
			return (
					result,
					partial
			) ->
			{
				result.addAll(partial);
				return result;
			};
		}

		@Override
		public Function<SortedSet<E>, SortedSet<E>> finisher() {
			return Function.identity();
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Set.of(Characteristics.IDENTITY_FINISH);
		}

	}

	/**
	 * A {@link Collector} that yields a {@link String} built by concatenating
	 * the values returned by {@link Object#toString()} when invoked on the
	 * elements of the stream.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 * 
	 * @param <E> the type of the elements to be sorted
	 */
	public static class StringCollector<E> implements Collector<E, StringBuilder, String> {

		private final String separator;

		/**
		 * Builds the collector.
		 * 
		 * @param separator the separator to use when concatenating items.
		 */
		public StringCollector(
				String separator) {
			this.separator = separator;
		}

		@Override
		public Supplier<StringBuilder> supplier() {
			return () -> new StringBuilder();
		}

		@Override
		public BiConsumer<StringBuilder, E> accumulator() {
			return (
					builder,
					e
			) ->
			{
				if (builder.length() == 0)
					builder.append(e);
				else
					builder.append(separator).append(e);
			};
		}

		@Override
		public BinaryOperator<StringBuilder> combiner() {
			return (
					result,
					partial
			) -> result.append(partial);
		}

		@Override
		public Function<StringBuilder, String> finisher() {
			return builder -> builder.toString();
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Collections.emptySet();
		}

	}

}
