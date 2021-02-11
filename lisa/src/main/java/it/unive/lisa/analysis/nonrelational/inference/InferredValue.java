package it.unive.lisa.analysis.nonrelational.inference;

import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A value that can be inferred by {@link InferenceSystem}s. This builds on top
 * of {@link NonRelationalDomain}, adding
 * {@link #variable(Identifier, ProgramPoint)} to force information stored into
 * variables to predetermined data if needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of inferred value
 */
public interface InferredValue<T extends InferredValue<T>>
		extends NonRelationalDomain<T, ValueExpression, InferenceSystem<T>> {

	/**
	 * Yields a fixed abstraction of the given variable. The abstraction does
	 * not depend on the abstract values that get assigned to the variable, but
	 * is instead fixed among all possible execution paths. If this method does
	 * not return the bottom element (as the default implementation does), then
	 * {@link InferenceSystem#assign(Identifier, it.unive.lisa.symbolic.SymbolicExpression, ProgramPoint)}
	 * will store that abstract element instead of the one computed starting
	 * from the expression.
	 * 
	 * @param id The identifier representing the variable being assigned
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the fixed abstraction of the variable
	 */
	default T variable(Identifier id, ProgramPoint pp) {
		return bottom();
	}

	/**
	 * Yields the execution state, that will be stored inside the
	 * {@link InferenceSystem}, that represent the state after the semantics
	 * computation that led to the creation of this inferred value.
	 * 
	 * @return the new execution state
	 */
	T executionState();
}
