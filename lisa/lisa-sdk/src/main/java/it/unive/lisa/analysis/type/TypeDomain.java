package it.unive.lisa.analysis.type;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.DomainWithReplacement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A domain that is able to determine the runtime types of an expression given
 * the runtime types of its operands. A type domain can handle instances of
 * {@link ValueExpression}s, and are associated to {@link TypeLattice}
 * instances.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link TypeLattice} that this domain works with
 */
public interface TypeDomain<L extends TypeLattice<L>> extends DomainWithReplacement<L, ValueExpression> {

	/**
	 * Yields the runtime types that this analysis infers for the given
	 * expression.
	 * 
	 * @param state  the state of the domain to be queried
	 * @param e      the expression to type
	 * @param pp     the program point where the types are required
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the runtime types
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Set<Type> getRuntimeTypesOf(
			L state,
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

	/**
	 * Yields the dynamic type that this analysis infers for the given
	 * expression. The dynamic type is the least common supertype of all its
	 * runtime types.
	 * 
	 * @param state  the state of the domain to be queried
	 * @param e      the expression to type
	 * @param pp     the program point where the types are required
	 * @param oracle the oracle for inter-domain communication
	 * 
	 * @return the dynamic type
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	Type getDynamicTypeOf(
			L state,
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;

}
