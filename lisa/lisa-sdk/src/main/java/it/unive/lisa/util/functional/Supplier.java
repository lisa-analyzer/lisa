package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.Supplier} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the type of results supplied by this supplier
 * @param <E> the type of exception that can be raised by the execution of this
 *                supplier
 */
@FunctionalInterface
public interface Supplier<T, E extends Exception> {

	/**
	 * Gets a result.
	 *
	 * @return a result
	 * 
	 * @throws E if the execution of this supplier raises an error
	 */
	T get()
			throws E;

}
