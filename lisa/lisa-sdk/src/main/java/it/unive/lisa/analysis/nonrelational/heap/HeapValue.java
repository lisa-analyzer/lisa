package it.unive.lisa.analysis.nonrelational.heap;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collection;

/**
 * A {@link Lattice} that represents a property of the memory of the program.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of the heap value
 */
public interface HeapValue<L extends HeapValue<L>>
		extends
		Lattice<L> {

	/**
	 * Yields the set of {@link Identifier}s that are reachable only from the
	 * given {@link Identifier} in the given state. Information in the receiver
	 * of the call should be ignored, as it is used as a singleton to invoke
	 * this method.
	 * 
	 * @param <F>   the type of the functional lattice that is used to represent
	 *                  the state
	 * @param state the state from which to compute the reachable identifiers
	 * @param id    the identifier for which to compute the reachable
	 *                  identifiers
	 * 
	 * @return a collection of identifiers that are reachable only from the
	 *             given identifier in the given state
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public <F extends FunctionalLattice<F, Identifier, L>> Collection<Identifier> reachableOnlyFrom(
			F state,
			Identifier id)
			throws SemanticException;

}
