package it.unive.lisa.util.collections.workset;

import java.util.Deque;
import java.util.LinkedList;

/**
 * A last-in, first-out working set. This implementation is <b>not</b>
 * thread-safe.
 * 
 * @author Luca Negrini
 * 
 * @param <E> the type of the elements that this working set contains
 */
public final class LIFOWorkingSet<E> implements WorkingSet<E> {

	private final Deque<E> ws;

	private LIFOWorkingSet() {
		ws = new LinkedList<>();
	}

	/**
	 * Yields a new, empty working set.
	 * 
	 * @param <E> the type of the elements that the returned working set
	 *                contains
	 * 
	 * @return the new working set
	 */
	public static <E> LIFOWorkingSet<E> mk() {
		return new LIFOWorkingSet<>();
	}

	@Override
	public void push(E e) {
		ws.push(e);
	}

	@Override
	public E pop() {
		return ws.pop();
	}

	@Override
	public E peek() {
		return ws.peek();
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
		LIFOWorkingSet<?> other = (LIFOWorkingSet<?>) obj;
		if (ws == null) {
			if (other.ws != null)
				return false;
		} else if (!ws.equals(other.ws))
			return false;
		return true;
	}
}
