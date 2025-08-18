package it.unive.lisa.program.cfg.statement;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.literal.Literal;

/**
 * A statement that ends the execution of a {@link CFG} yielding a value, either
 * returned or thrown.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface YieldsValue {

	/**
	 * Yields the value that this statement yields.
	 * 
	 * @return the value that this statement yields
	 */
	Expression yieldedValue();

	/**
	 * Yields whether this statement yields a value that is atomic, i.e., it is
	 * either a variable reference or a literal. If this method returns
	 * {@code false}, the yielded value may be a more complex expression, such
	 * as a binary operation, a function call, etc.
	 * 
	 * @return {@code true} if the yielded value is atomic, {@code false}
	 *             otherwise
	 */
	default boolean isAtomic() {
		Expression value = yieldedValue();
		return value instanceof VariableRef || value instanceof Literal;
	}

	/**
	 * Yields a new instance of this class that yields the given value. Apart
	 * from the value, the new instance will be equal to this one.
	 * 
	 * @param value the value that this instance will yield
	 * 
	 * @return a new instance of this class that yields the given value
	 */
	Statement withValue(
			Expression value);
}
