package it.unive.lisa.util.collections.workset;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * A LIFO working set that guarantees that each element will be added to this
 * working set no more than once. It works by pushing elements <i>only</i> if
 * they were not already added before (even if they have already been popped
 * out). This implementation is <b>not</b> thread-safe.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of the elements that this working set contains
 */
public class VisitOnceLIFOWorkingSet<E> implements VisitOnceWorkingSet<E> {

	private final Deque<E> ws;

	private final Collection<E> seen;

	/**
	 * Yields a new, empty working set.
	 * 
	 * @param <E> the type of the elements that the returned working set
	 *                contains
	 * 
	 * @return the new working set
	 */
	public static <E> VisitOnceLIFOWorkingSet<E> mk() {
		return new VisitOnceLIFOWorkingSet<>();
	}

	private VisitOnceLIFOWorkingSet() {
		ws = new LinkedList<>();
		seen = new HashSet<>();
	}

	@Override
	public void push(
			E e) {
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
	public Collection<E> getContents() {
		return ws;
	}

	@Override
	public String toString() {
		return ws.toString();
	}

	@Override
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
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VisitOnceLIFOWorkingSet<?> other = (VisitOnceLIFOWorkingSet<?>) obj;
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
