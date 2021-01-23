package it.unive.lisa.util.collections;

import java.util.Collection;
import java.util.Comparator;

/**
 * Utility methods for operations on {@link Collection}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Utils {

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
}
