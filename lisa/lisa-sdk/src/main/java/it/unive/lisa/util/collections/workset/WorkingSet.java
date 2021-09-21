package it.unive.lisa.util.collections.workset;

import it.unive.lisa.AnalysisSetupException;
import java.lang.reflect.InvocationTargetException;

/**
 * A working set, containing items to be processed.
 * 
 * @author Luca Negrini
 * 
 * @param <E> the type of the elements that this working set contains
 */
public interface WorkingSet<E> {

	/**
	 * Yields a {@link WorkingSet} instance by invoking a static {@code mk()}
	 * method on the given class.
	 * 
	 * @param <E>   the type of elements contained in the working set
	 * @param clazz the class of the working set to create
	 * 
	 * @return an instance of working set of type {@code clazz}
	 * 
	 * @throws AnalysisSetupException if the working set cannot be created
	 */
	@SuppressWarnings("unchecked")
	public static <E> WorkingSet<E> of(Class<? extends WorkingSet<E>> clazz) throws AnalysisSetupException {
		if (!WorkingSet.class.isAssignableFrom(clazz))
			throw new AnalysisSetupException(clazz + " is not a working set");

		try {
			return (WorkingSet<E>) clazz.getMethod("mk").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new AnalysisSetupException("Unable to create an instance of " + clazz.getName(), e);
		}
	}

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
