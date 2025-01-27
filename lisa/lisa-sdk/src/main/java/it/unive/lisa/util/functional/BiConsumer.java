package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.BiConsumer} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <E> the type of exception that can be raised by the execution of this
 *                consumer
 */
@FunctionalInterface
public interface BiConsumer<T, U, E extends Exception> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param t the first input argument
	 * @param u the second input argument
	 * 
	 * @throws E if the execution of this consumer raises an error
	 */
	void accept(
			T t,
			U u)
			throws E;
}
