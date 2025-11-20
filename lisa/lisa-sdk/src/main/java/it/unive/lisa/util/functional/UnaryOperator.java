package it.unive.lisa.util.functional;

/**
 * Mirror of {@link java.util.function.UnaryOperator} that can raise arbitrary
 * exceptions, declared as part of this interface's type signature.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T> the type of the operand and result of the operator
 * @param <E> the type of exception that can be raised by the execution of this
 *                operator
 */
@FunctionalInterface
public interface UnaryOperator<T, E extends Exception>
		extends
		Function<T, T, E> {

}
