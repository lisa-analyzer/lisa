package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

/**
 * A unary {@link Operator} that can be applied to a single
 * {@link SymbolicExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface UnaryOperator extends Operator {

	/**
	 * Computes the runtime types of this expression (i.e., of the result of
	 * this expression) assuming that the argument of this expression has the
	 * given types.
	 * 
	 * @param types    the type system knowing about the types of the current
	 *                     program
	 * @param argument the set of types of the argument of this expression
	 * 
	 * @return the runtime types of this expression
	 */
	Set<Type> typeInference(TypeSystem types, Set<Type> argument);
}
