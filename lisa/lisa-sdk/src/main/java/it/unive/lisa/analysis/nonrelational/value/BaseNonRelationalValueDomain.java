package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.nonrelational.BaseNonRelationalDomain;

/**
 * Base implementation for {@link NonRelationalValueDomain}s, offering all
 * capabilities of {@link BaseNonRelationalDomain}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of lattice used as values in environments produced by
 *                this domain
 */
public interface BaseNonRelationalValueDomain<
		L extends Lattice<L>>
		extends
		BaseNonRelationalDomain<L, ValueEnvironment<L>>,
		NonRelationalValueDomain<L> {

	@Override
	default ValueEnvironment<L> makeLattice() {
		return new ValueEnvironment<>(top());
	}

}
