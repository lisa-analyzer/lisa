package it.unive.lisa.util.collections.workset;

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
public final class VisitOnceWorkingSet<E> implements WorkingSet<E> {

	private final WorkingSet<E> ws;

	private final Collection<E> seen;

	private VisitOnceWorkingSet(WorkingSet<E> ws) {
		this.ws = ws;
		this.seen = Collections.newSetFromMap(new IdentityHashMap<>());
	}

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

	/**
	 * Yields the elements visited (and this no longer able to be added to this
	 * working set) by this object.
	 * 
	 * @return the collection of visited elements
	 */
	public Collection<E> getSeen() {
		return seen;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((seen == null) ? 0 : seen.hashCode());
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
		VisitOnceWorkingSet<?> other = (VisitOnceWorkingSet<?>) obj;
		if (seen == null) {
			if (other.seen != null)
				return false;
		} else if (!seen.equals(other.seen))
			return false;
		if (ws == null) {
			if (other.ws != null)
				return false;
		} else if (!ws.equals(other.ws))
			return false;
		return true;
	}
}
