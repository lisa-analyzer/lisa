package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * An abstract domain of the analysis. Instances of this class must process
 * information about the whole program memory: values, types, and memory
 * structure should be modeled and taken into account by this domain.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of {@link AbstractLattice} that this domain works with
 */
public interface AbstractDomain<L extends AbstractLattice<L>>
		extends
		SemanticDomain<L, L, SymbolicExpression, Identifier> {

	/**
	 * Builds a {@link SemanticOracle} that can be used either by sub-domains or
	 * by the analysis itself to query the current state of the analysis.
	 *
	 * @param state the current state of the analysis
	 * 
	 * @return a {@link SemanticOracle} instance
	 */
	SemanticOracle makeOracle(
			L state);

}
