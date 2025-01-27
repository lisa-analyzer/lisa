package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.Function} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception that can be raised by the execution of this
 *                function
 */
@FunctionalInterface
public interface Function<T, R, E extends Exception> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * 
	 * @return the function result
	 * 
	 * @throws E if the execution of this function raises an error
	 */
	R apply(
			T t)
			throws E;
}
