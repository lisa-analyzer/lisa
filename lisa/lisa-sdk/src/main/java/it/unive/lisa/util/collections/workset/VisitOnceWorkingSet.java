package it.unive.lisa.util.collections.workset;

import java.util.Collection;

/**
 * A working set, containing items to be processed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <E> the type of the elements that this working set contains
 */
public interface VisitOnceWorkingSet<E> extends WorkingSet<E> {

	/**
	 * Yields the elements visited (and thus no longer able to be added to this
	 * working set) by this object.
	 * 
	 * @return the collection of visited elements
	 */
	Collection<E> getSeen();
}
