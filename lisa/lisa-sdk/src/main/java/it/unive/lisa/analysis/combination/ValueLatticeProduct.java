package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A {@link ValueCartesianCombination} of two arbitrary {@link ValueLattice}s,
 * with no reduction.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T1> the type of the first lattice
 * @param <T2> the type of the second lattice
 */
public class ValueLatticeProduct<T1 extends ValueLattice<T1>,
		T2 extends ValueLattice<T2>> extends ValueCartesianCombination<ValueLatticeProduct<T1, T2>, T1, T2> {

	/**
	 * Builds a new product of two lattices.
	 * 
	 * @param first  the first lattice
	 * @param second the second lattice
	 */
	public ValueLatticeProduct(
			T1 first,
			T2 second) {
		super(first, second);
	}

	@Override
	public ValueLatticeProduct<T1, T2> mk(
			T1 first,
			T2 second) {
		return new ValueLatticeProduct<>(first, second);
	}

	@Override
	public ValueLatticeProduct<T1, T2> store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return new ValueLatticeProduct<>(first.store(target, source), second.store(target, source));
	}

}
