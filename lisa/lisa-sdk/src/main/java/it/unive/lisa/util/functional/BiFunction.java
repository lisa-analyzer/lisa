package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.BiFunction} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception that can be raised by the execution of this
 *                function
 */
@FunctionalInterface
public interface BiFunction<T,
		U,
		R,
		E extends Exception> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * 
	 * @return the function result
	 * 
	 * @throws E if the execution of this function raises an error
	 */
	R apply(
			T t,
			U u)
			throws E;

}
