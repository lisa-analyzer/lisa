package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.numeric.IntInterval;

/**
 * Interface for a numeric analysis that exposes utility methods to compute
 * semantics operations.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <I> the concrete type of the instances of this domain
 */
public interface SmashedSumIntDomain<I extends SmashedSumIntDomain<I>>
		extends
		BaseNonRelationalValueDomain<I> {

	/**
	 * Creates a new instance of this domain from the given interval.
	 */
	I fromInterval(IntInterval intv)
		throws SemanticException;

	/**
	 * Converts this domain instance to an interval.
	 */
	IntInterval toInterval()
		throws SemanticException;
}
