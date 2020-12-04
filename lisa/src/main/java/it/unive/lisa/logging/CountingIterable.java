package it.unive.lisa.logging;

import java.util.Iterator;

/**
 * An {@link Iterable} that wraps onto another one, causing the associated
 * {@link Counter} to progress each time the iterator underlying this iterable
 * is advances.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of elements returned by the iterator
 */
final class CountingIterable<E> implements Iterable<E> {

	/**
	 * The iterator that wraps the original one
	 */
	private final Iterator<E> iterator;

	/**
	 * Builds the iterable wrapping around the given one, pairing with the given
	 * counter.
	 * 
	 * @param iterable the iterable to wrap
	 * @param counter  the counter to use
	 */
	CountingIterable(Iterable<E> iterable, Counter counter) {
		Iterator<E> it = iterable.iterator();
		this.iterator = new Iterator<E>() {

			@Override
			public boolean hasNext() {
				if (it.hasNext())
					return true;
				else {
					counter.off();
					return false;
				}
			}

			@Override
			public E next() {
				if (!counter.isLogging())
					counter.on();

				counter.count();
				return it.next();
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}

	@Override
	public Iterator<E> iterator() {
		return iterator;
	}
}
