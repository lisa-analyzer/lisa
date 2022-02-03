package it.unive.lisa.util.collections;

import java.util.Iterator;

public class CastIterable<E, T extends E> implements Iterable<T> {

	private final Iterable<E> parent;
	private final Class<T> type;

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
