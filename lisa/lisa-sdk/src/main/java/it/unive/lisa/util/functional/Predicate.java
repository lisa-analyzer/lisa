package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.Predicate} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the type of the input to the predicate
 * @param <E> the type of exception that can be raised by the execution of this
 *                predicate
 */
@FunctionalInterface
public interface Predicate<T, E extends Exception> {

	/**
	 * Evaluates this predicate on the given argument.
	 *
	 * @param t the input argument
	 * 
	 * @return {@code true} if the input argument matches the predicate,
	 *             otherwise {@code false}
	 * 
	 * @throws E if the execution of this predicate raises an error
	 */
	boolean test(
			T t)
			throws Exception;
}
