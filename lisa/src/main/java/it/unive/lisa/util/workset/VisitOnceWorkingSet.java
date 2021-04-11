package it.unive.lisa.util.workset;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;

public class VisitOnceWorkingSet<E> implements WorkingSet<E> {

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
