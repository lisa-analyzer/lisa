package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.Lattice;

/**
 * A {@link CartesianCombination} of two arbitrary {@link Lattice}s, with no
 * reduction.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T1> the type of the first lattice
 * @param <T2> the type of the second lattice
 */
public class LatticeProduct<T1 extends Lattice<T1>,
		T2 extends Lattice<T2>>
		extends
		CartesianCombination<LatticeProduct<T1, T2>, T1, T2> {

	/**
	 * Builds a new product of two lattices.
	 * 
	 * @param first  the first lattice
	 * @param second the second lattice
	 */
	public LatticeProduct(
			T1 first,
			T2 second) {
		super(first, second);
	}

	@Override
	public LatticeProduct<T1, T2> mk(
			T1 first,
			T2 second) {
		return new LatticeProduct<>(first, second);
	}

}
