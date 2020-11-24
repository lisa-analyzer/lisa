package it.unive.lisa.util.workset;

import java.util.Deque;
import java.util.LinkedList;

/**
 * A first-in, first-out working set. This implementation is <b>not</b>
 * thread-safe.
 * 
 * @author Luca Negrini
 * @param <E> the type of the elements that this working set contains
 */
public class FIFOWorkingSet<E> implements WorkingSet<E> {

	/**
	 * Yields a new, empty working set.
	 * 
	 * @param <E> the type of the elements that the returned working set contains
	 * @return the new working set
	 */
	public static <E> FIFOWorkingSet<E> mk() {
		return new FIFOWorkingSet<>();
	}

	private final Deque<E> ws;

	private FIFOWorkingSet() {
		ws = new LinkedList<>();
	}

	@Override
	public void push(E e) {
		ws.addLast(e);
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
