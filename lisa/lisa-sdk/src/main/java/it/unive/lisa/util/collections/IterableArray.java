package it.unive.lisa.util.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterable over an array of elements.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the element type of the underlying array
 */
public class IterableArray<E> implements Iterable<E> {

	/**
	 * The underlying array
	 */
	private final E[] array;

	/**
	 * Builds the iterable.
	 * 
	 * @param array the underlying array
	 */
	public IterableArray(
			E[] array) {
		this.array = array;
	}

	/**
	 * Yields the size of this iterable, that is, the length of the array behind
	 * this iterable.
	 * 
	 * @return the size
	 */
	public int size() {
		return array.length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(array);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IterableArray<?> other = (IterableArray<?>) obj;
		if (!Arrays.deepEquals(array, other.array))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Arrays.toString(array);
	}

	@Override
	public Iterator<E> iterator() {
		return new IteratorFromArray();
	}

	private final class IteratorFromArray implements Iterator<E> {

		private int pos;

		private IteratorFromArray() {
		}

		@Override
		public boolean hasNext() {
			return pos < array.length;
		}

		@Override
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException("End of the array reached");

			return array[pos++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Cannot remove elements from an iterator built over an array");
		}

	}

}
