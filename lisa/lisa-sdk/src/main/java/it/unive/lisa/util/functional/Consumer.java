package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.Consumer} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the type of the input to the operation
 * @param <E> the type of exception that can be raised by the execution of this
 *                consumer
 */
@FunctionalInterface
public interface Consumer<T,
		E extends Exception> {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param t the input argument
	 * 
	 * @throws E if the execution of this consumer raises an error
	 */
	void accept(
			T t)
			throws E;

}
