package it.unive.lisa.util.collections;

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
	public IterableArray(E[] array) {
		this.array = array;
	}

	@Override
	public Iterator<E> iterator() {
		return new IteratorFromArray();
	}

	private class IteratorFromArray implements Iterator<E> {

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