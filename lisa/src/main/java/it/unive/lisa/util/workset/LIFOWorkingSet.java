package it.unive.lisa.util.workset;

import java.util.Stack;

/**
 * A last-in, first-out working set. This implementation is <b>not</b>
 * thread-safe.
 * 
 * @author Luca Negrini
 * 
 * @param <E> the type of the elements that this working set contains
 */
public class LIFOWorkingSet<E> implements WorkingSet<E> {

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

	private final Stack<E> ws;

	private LIFOWorkingSet() {
		ws = new Stack<>();
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
}
