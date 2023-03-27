package it.unive.lisa.analysis.value;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An domain that is able to determine the runtime types of an expression given
 * the runtime types of its operands.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of the {@link TypeDomain}
 */
public interface TypeDomain<T extends TypeDomain<T>> extends ValueDomain<T> {

	/**
	 * Yields the runtime types that this analysis infers for the given
	 * expression.
	 * 
	 * @param e  the expression to type
	 * @param pp the program point where the types are required
	 * 
	 * @return the runtime types
	 */
	Set<Type> getRuntimeTypesOf(ValueExpression e, ProgramPoint pp);

	/**
	 * Yields the dynamic type that this analysis infers for the given
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @param e  the expression to type
	 * @param pp the program point where the types are required
	 * 
	 * @return the dynamic type
	 */
	Type getDynamicTypeOf(ValueExpression e, ProgramPoint pp);
}
