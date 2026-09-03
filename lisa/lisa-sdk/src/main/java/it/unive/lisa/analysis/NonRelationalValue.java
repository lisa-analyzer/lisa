package it.unive.lisa.analysis;

/**
 * A lattice element representing the non-relational abstract value of a single
 * expression, used as a common type so that different kinds of value domains
 * (numeric, string, taint, ...) can be compared and combined.
 *
 * @author <a href="mailto:giacomo.boldini@unive.it">Giacomo Boldini</a>
 *
 * @param <L> the concrete type of this lattice
 */
public interface NonRelationalValue<L extends NonRelationalValue<L>>
		extends
		Lattice<L> {

	// TODO define set minus (for NSAD 19)
	// minus(... ,pp, oracle) throws SemanticException;

}
