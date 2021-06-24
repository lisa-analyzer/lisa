package it.unive.lisa.util.collections.workset;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A last-in, first-out working set. This implementation is thread-safe.
 * 
 * @author Luca Negrini
 * 
 * @param <E> the type of the elements that this working set contains
 */
public final class ConcurrentLIFOWorkingSet<E> implements WorkingSet<E> {

	private final Deque<E> ws;

	private ConcurrentLIFOWorkingSet() {
		ws = new ConcurrentLinkedDeque<>();
	}
	
	/**
	 * Yields a new, empty working set.
	 * 
	 * @param <E> the type of the elements that the returned working set
	 *                contains
	 * 
	 * @return the new working set
	 */
	public static <E> ConcurrentLIFOWorkingSet<E> mk() {
		return new ConcurrentLIFOWorkingSet<>();
	}

	@Override
	public void push(E e) {
		ws.addFirst(e);
	}

	@Override
	public E pop() {
		return ws.removeFirst();
	}

	@Override
	public E peek() {
		return ws.peekFirst();
	}

	@Override
	public int size() {
		return ws.size();
	}

	@Override
	public boolean isEmpty() {
		return ws.isEmpty();
	}

	@Override
	public String toString() {
		return ws.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ws == null) ? 0 : ws.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConcurrentLIFOWorkingSet<?> other = (ConcurrentLIFOWorkingSet<?>) obj;
		if (ws == null) {
			if (other.ws != null)
				return false;
		} else if (!ws.equals(other.ws))
			return false;
		return true;
	}
}
