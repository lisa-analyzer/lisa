package it.unive.lisa.analysis.type;

import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

/**
 * An oracle that can be queried for runtime type information.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface TypeOracle {

	/**
	 * Yields the runtime types that this analysis infers for the given
	 * expression.
	 * 
	 * @param e      the expression to type
	 * @param pp     the program point where the types are required
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the runtime types
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Set<Type> getRuntimeTypesOf(SymbolicExpression e, ProgramPoint pp, SemanticOracle oracle) throws SemanticException;

	/**
	 * Yields the dynamic type that this analysis infers for the given
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @param e      the expression to type
	 * @param pp     the program point where the types are required
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the dynamic type
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Type getDynamicTypeOf(SymbolicExpression e, ProgramPoint pp, SemanticOracle oracle) throws SemanticException;
}
