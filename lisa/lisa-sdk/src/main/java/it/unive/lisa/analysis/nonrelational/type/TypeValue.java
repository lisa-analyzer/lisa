package it.unive.lisa.analysis.nonrelational.type;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * A {@link Lattice} that represents a set of types corresponding to the runtime
 * types of an expression.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the concrete type of the lattice
 */
public interface TypeValue<L extends TypeValue<L>>
		extends
		Lattice<L> {

	/**
	 * Yields the set containing the types held by this instance.
	 * 
	 * @return the set of types inside this instance
	 */
	Set<Type> getRuntimeTypes();

}
