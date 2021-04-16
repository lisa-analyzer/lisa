package it.unive.lisa.util.workset;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * A working set that guarantees that each element will be added to this working
 * set no more than once. It works by wrapping a difference instance of
 * {@link WorkingSet}, and delegating all operations to that instance, except
 * for {@link #push(Object)}: an element will be pushed <i>only</i> if it has
 * not already added before (even if it is has already been popped out). This
 * implementation is <b>not</b> thread-safe.
 * 
 * @author Luca Negrini
 * 
 * @param <E> the type of the elements that this working set contains
 */
public class VisitOnceWorkingSet<E> implements WorkingSet<E> {

	/**
	 * Yields a new, empty working set.
	 * 
	 * @param <E> the type of the elements that the returned working set
	 *                contains
	 * @param ws  the underlying working set
	 * 
	 * @return the new working set
	 */
	public static <E> VisitOnceWorkingSet<E> mk(WorkingSet<E> ws) {
		return new VisitOnceWorkingSet<>(ws);
	}

	private final WorkingSet<E> ws;

	private final Collection<E> seen;

	private VisitOnceWorkingSet(WorkingSet<E> ws) {
		this.ws = ws;
		this.seen = Collections.newSetFromMap(new IdentityHashMap<>());
	}

	@Override
	public void push(E e) {
		if (seen.contains(e))
			return;

		seen.add(e);
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
