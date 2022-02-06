package it.unive.lisa.util.collections;

import java.util.Iterator;

/**
 * An iterable that wraps another one, and whose iterator delegates to the
 * wrapped one's, but returns its elements casted to another type.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of elements contained in the iterable to be wrapped
 * @param <T> the type to cast the elements to
 */
public class CastIterable<E, T extends E> implements Iterable<T> {

	private final Iterable<E> parent;
	private final Class<T> type;

	/**
	 * Builds the iterable.
	 * 
	 * @param parent the iterable to wrap into this one
	 * @param type   the class to which elements of the wrapped iterable should
	 *                   be casted to
	 */
	public CastIterable(Iterable<E> parent, Class<T> type) {
		this.parent = parent;
		this.type = type;
	}

	@Override
	public Iterator<T> iterator() {
		return new CastIterator(parent.iterator());
	}

	private class CastIterator implements Iterator<T> {

		private final Iterator<E> wrapped;

		public CastIterator(Iterator<E> wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public boolean hasNext() {
			return wrapped.hasNext();
		}

		@Override
		public T next() {
			return type.cast(wrapped.next());
		}

		@Override
		public void remove() {
			wrapped.remove();
		}
	}
}
