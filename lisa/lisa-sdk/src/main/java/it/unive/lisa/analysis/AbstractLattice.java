package it.unive.lisa.analysis;

/**
 * An abstract lattice containing the abstract information computed during the
 * analysis. Instances of this class must hold information about the whole
 * program memory: values, types, and memory structure should be modeled by this
 * lattice.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of {@link AbstractLattice}
 */
public interface AbstractLattice<L extends AbstractLattice<L>>
		extends
		DomainLattice<L, L> {

	/**
	 * Yields a copy of this state, but with its memory abstraction set to top.
	 * This is useful to represent effects of unknown calls that arbitrarily
	 * manipulate the memory.
	 * 
	 * @return the copy with top memory
	 */
	L withTopMemory();

	/**
	 * Yields a copy of this state, but with its value abstraction set to top.
	 * This is useful to represent effects of unknown calls that arbitrarily
	 * manipulate the values of variables.
	 * 
	 * @return the copy with top values
	 */
	L withTopValues();

	/**
	 * Yields a copy of this state, but with its type abstraction set to top.
	 * This is useful to represent effects of unknown calls that arbitrarily
	 * manipulate the values of variables (and their type accordingly).
	 * 
	 * @return the copy with top types
	 */
	L withTopTypes();

}
