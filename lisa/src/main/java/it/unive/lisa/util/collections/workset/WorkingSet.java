package it.unive.lisa.util.collections.workset;

/**
 * A working set, containing items to be processed.
 * 
 * @author Luca Negrini
 * 
 * @param <E> the type of the elements that this working set contains
 */
public interface WorkingSet<E> {

	/**
	 * Pushes a new element into this working set.
	 * 
	 * @param e the element
	 */
	void push(E e);

	/**
	 * Removes the next element to be processed from this working set and
	 * returns it.
	 * 
	 * @return the next element to process
	 */
	E pop();

	/**
	 * Returns the next element to be processed from this working set without
	 * removing it.
	 * 
	 * @return the next element to process
	 */
	E peek();

	/**
	 * Yields the size of this working set, that is, the number of elements
	 * contained in it.
	 * 
	 * @return the size
	 */
	int size();

	/**
	 * Yields {@code true} if and only if this working set is empty.
	 * 
	 * @return {@code true} if that condition holds
	 */
	boolean isEmpty();
}
