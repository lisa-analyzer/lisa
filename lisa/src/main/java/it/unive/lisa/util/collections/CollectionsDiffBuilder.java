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

public class CollectionsDiffBuilder<T> {

	private final Class<T> elementType;
	private final Collection<T> first;
	private final Collection<T> second;
	private final List<T> onlyFirst = new ArrayList<>();
	private final List<Pair<T, T>> commons = new ArrayList<>();
	private final List<T> onlySecond = new ArrayList<>();

	public CollectionsDiffBuilder(Class<T> elementType, Collection<T> first, Collection<T> second) {
		this.elementType = elementType;
		this.first = first;
		this.second = second;
	}

	@SuppressWarnings("unchecked")
	public void computeDiff(Comparator<T> comparer) {
		Deque<T> f = new ArrayDeque<>();
		Deque<T> s = new ArrayDeque<>();

		T[] fArray = first.toArray((T[]) Array.newInstance(elementType, first.size()));
		T[] sArray = second.toArray((T[]) Array.newInstance(elementType, first.size()));
		Arrays.sort(fArray, comparer);
		Arrays.sort(sArray, comparer);
		f.addAll(Arrays.asList(fArray));
		s.addAll(Arrays.asList(sArray));

		computeQueueDiff(f, s, comparer);
	}

	private void computeQueueDiff(Deque<T> f, Deque<T> s, Comparator<T> comparer) {
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

	public Collection<T> getOnlySecond() {
		return onlySecond;
	}

	public Collection<T> getOnlyFirst() {
		return onlyFirst;
	}

	public Collection<Pair<T, T>> getCommons() {
		return commons;
	}
	
	public boolean sameContent() {
		return onlyFirst.isEmpty() && onlySecond.isEmpty();
	}
}
