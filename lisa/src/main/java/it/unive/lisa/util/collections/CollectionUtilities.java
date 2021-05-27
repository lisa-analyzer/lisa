package it.unive.lisa.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Utility methods for operations on {@link Collection}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CollectionUtilities {

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
	public static <T> int nullSafeCompare(boolean nullFirst, T left, T right, Comparator<T> comparator) {
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
	public static <T, C extends Collection<T>> boolean equals(C first, C second, BiPredicate<T, T> equalityTest) {
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
	public static <T, C extends Collection<T>> void join(C first, C second, C result, BiPredicate<T, T> equalityTest,
			BiFunction<T, T, T> joiner) {
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

	@SafeVarargs
	public static <T> Collection<T> collect(T... objs) {
		ArrayList<T> res = new ArrayList<>(objs.length);
		for (T o : objs)
			res.add(o);
		return res;
	}
}
