package it.unive.lisa.util.workset;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A last-in, first-out working set. This implementation is thread-safe.
 * 
 * @author Luca Negrini
 * 
 * @param <E> the type of the elements that this working set contains
 */
public class ConcurrentLIFOWorkingSet<E> implements WorkingSet<E> {

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

	private final Deque<E> ws;

	private ConcurrentLIFOWorkingSet() {
		ws = new ConcurrentLinkedDeque<>();
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
}
