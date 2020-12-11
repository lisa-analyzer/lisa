package it.unive.lisa.util.collections;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An utility class that can compute the difference between two collections
 * containing the same object types. Common objects will can be retrieved
 * through {@link #getCommons()}, while ones available only in one of them can
 * be retrieved through {@link #getOnlyFirst()} and {@link #getOnlySecond()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of elements within the collections to be compared
 */
public class CollectionsDiffBuilder<T> {

	private final Class<T> elementType;
	private final Collection<T> first;
	private final Collection<T> second;
	private final List<T> onlyFirst = new ArrayList<>();
	private final List<Pair<T, T>> commons = new ArrayList<>();
	private final List<T> onlySecond = new ArrayList<>();

	/**
	 * Builds the diff builder.
	 * 
	 * @param elementType the type of elements that are stored into the
	 *                        collections (used for internal temporary array
	 *                        creation)
	 * @param first       the first collection
	 * @param second      the second colelction
	 */
	public CollectionsDiffBuilder(Class<T> elementType, Collection<T> first, Collection<T> second) {
		this.elementType = elementType;
		this.first = first;
		this.second = second;
	}

	/**
	 * Computes the diff between the collections used to create this object.
	 * After sorting the two collections independently according to the given
	 * {@link Comparator}, the two collections are simultaneously iterated over,
	 * and, for each pair {@code <f, s>} where {@code f} comes from the first
	 * collection and {@code s} comes from the second one, {@code comparer} is
	 * used to determine the relationship of the two elements, exploiting their
	 * ordering:
	 * <ul>
	 * <li>if both {@code f} and {@code s} are {@code null}, the comparison
	 * interrupts</li>
	 * <li>if {@code f} is {@code null} and {@code s} is not, {@code s} is added
	 * to {@link #getOnlySecond()} and the iterator of the second collection
	 * moves forward</li>
	 * <li>if {@code s} is {@code null} and {@code f} is not, {@code f} is added
	 * to {@link #getOnlyFirst()} and the iterator of the first collection moves
	 * forward</li>
	 * <li>if {@code comparer.compare(f, s) == 0}, {@code <f, s>} is added to
	 * {@link #getCommons()} and the iterators of both collections move
	 * forward</li>
	 * <li>if {@code comparer.compare(f, s) > 0}, {@code s} is added to
	 * {@link #getOnlySecond()} and the iterator of the second collection moves
	 * forward</li>
	 * <li>if {@code comparer.compare(f, s) < 0}, {@code f} is added to
	 * {@link #getOnlyFirst()} and the iterator of the first collection moves
	 * forward</li>
	 * </ul>
	 * 
	 * @param comparer the {@link Comparator} establishing the ordering between
	 *                     elements in the two collections
	 */
	@SuppressWarnings("unchecked")
	public void compute(Comparator<T> comparer) {
		commons.clear();
		onlyFirst.clear();
		onlySecond.clear();

		Deque<T> f = new ArrayDeque<>();
		Deque<T> s = new ArrayDeque<>();

		T[] fArray = first.toArray((T[]) Array.newInstance(elementType, first.size()));
		T[] sArray = second.toArray((T[]) Array.newInstance(elementType, second.size()));
		Arrays.sort(fArray, comparer);
		Arrays.sort(sArray, comparer);
		f.addAll(Arrays.asList(fArray));
		s.addAll(Arrays.asList(sArray));

		queueDiff(f, s, comparer);
	}

	private void queueDiff(Deque<T> f, Deque<T> s, Comparator<T> comparer) {
		T currentF = null;
		T currentS = null;

		while (!(f.isEmpty() && s.isEmpty())) {
			currentF = f.peek();
			currentS = s.peek();

			if (currentF == null) {
				if (currentS == null)
					break;
				else {
					onlySecond.add(currentS);
					s.remove();
					continue;
				}
			} else {
				if (currentS == null) {
					onlyFirst.add(currentF);
					f.remove();
					continue;
				}
			}

			int cmp = comparer.compare(currentF, currentS);
			if (cmp == 0) {
				commons.add(new ImmutablePair<>(currentF, currentS));
				f.remove();
				s.remove();
			} else if (cmp < 0) {
				onlyFirst.add(currentF);
				f.remove();
			} else {
				onlySecond.add(currentS);
				s.remove();
			}
		}
	}

	/**
	 * Yields a collection containing all the elements that are contained only
	 * in the second collection used to create this builder. The returned
	 * collection will be empty unless {@link #compute(Comparator)} has been
	 * called.
	 * 
	 * @return the elements contained only in the second collection
	 */
	public Collection<T> getOnlySecond() {
		return onlySecond;
	}

	/**
	 * Yields a collection containing all the elements that are contained only
	 * in the first collection used to create this builder. The returned
	 * collection will be empty unless {@link #compute(Comparator)} has been
	 * called.
	 * 
	 * @return the elements contained only in the first collection
	 */
	public Collection<T> getOnlyFirst() {
		return onlyFirst;
	}

	/**
	 * Yields a collection containing all the pair of elements (deemed to be
	 * equal) that are contained in both collections used to create this
	 * builder. {@link Pair#getLeft()} will return the element coming from the
	 * first collection, while {@link Pair#getRight()} will return the one
	 * coming from the second one. The returned collection will be empty unless
	 * {@link #compute(Comparator)} has been called.
	 * 
	 * @return the elements contained only in both collections
	 */
	public Collection<Pair<T, T>> getCommons() {
		return commons;
	}

	/**
	 * Yields {@code true} if and only if the two collections used to create
	 * this builder contain the same elements, that is, if both
	 * {@link #getOnlyFirst()} and {@link #getOnlySecond()} are empty. This
	 * method will always return {@code true} unless
	 * {@link #compute(Comparator)} has been called.
	 * 
	 * @return {@code true} if the collections contain the same elements,
	 *             {@code false} otherwise
	 */
	public boolean sameContent() {
		return onlyFirst.isEmpty() && onlySecond.isEmpty();
	}
}
