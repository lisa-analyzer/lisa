package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * Base implementation for {@link NonRelationalTypeDomain}s. This class extends
 * {@link BaseLattice} and implements
 * {@link NonRelationalTypeDomain#eval(SymbolicExpression, Environment, ProgramPoint, SemanticOracle)}
 * by taking care of the recursive computation of inner expressions evaluation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete type of this domain
 */
public interface BaseNonRelationalTypeDomain<T extends BaseNonRelationalTypeDomain<T>>
		extends
		BaseLattice<T>,
		BaseNonRelationalDomain<T, ValueExpression, TypeEnvironment<T>>,
		NonRelationalTypeDomain<T> {
	
	@Override
	default boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		// a type domain can process everything
		return true;
	}
}
