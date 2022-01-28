package it.unive.lisa.analysis.instances;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An analysis that is able to determine the runtime types of an expression
 * given the runtime types of its operands.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface TypeAnalysis {

	/**
	 * Yields the runtime types that this analysis inferred for the last
	 * computed expression.
	 * 
	 * @return the runtime types
	 */
	ExternalSet<Type> getInferredRuntimeTypes();

	/**
	 * Yields the dynamic type that this analysis inferred for the last computed
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @return the dynamic type
	 */
	Type getInferredDynamicType();
}
