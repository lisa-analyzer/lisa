package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * A binary {@link Operator} that can be applied to a pair of
 * {@link SymbolicExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface BinaryOperator extends Operator {

	/**
	 * Computes the runtime types of this expression (i.e., of the result of
	 * this expression) assuming that the arguments of this expression have the
	 * given types.
	 * 
	 * @param types the type system knowing about the types of the current
	 *                  program
	 * @param left  the set of types of the left-most argument of this
	 *                  expression
	 * @param right the set of types of the right-most argument of this
	 *                  expression
	 * 
	 * @return the runtime types of this expression
	 */
	Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right);
}
