package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.BiPredicate} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the type of the first argument to the predicate
 * @param <U> the type of the second argument the predicate
 * @param <E> the type of exception that can be raised by the execution of this
 *                predicate
 */
@FunctionalInterface
public interface BiPredicate<T, U, E extends Exception> {

	/**
	 * Evaluates this predicate on the given arguments.
	 *
	 * @param t the first input argument
	 * @param u the second input argument
	 * 
	 * @return {@code true} if the input arguments match the predicate,
	 *             otherwise {@code false}
	 * 
	 * @throws E if the execution of this predicate raises an error
	 */
	boolean test(
			T t,
			U u)
			throws E;

}
