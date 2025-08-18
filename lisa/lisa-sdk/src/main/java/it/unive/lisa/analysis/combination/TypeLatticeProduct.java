package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A {@link TypeCartesianCombination} of two arbitrary {@link TypeLattice}s,
 * with no reduction.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T1> the type of the first lattice
 * @param <T2> the type of the second lattice
 */
public class TypeLatticeProduct<T1 extends TypeLattice<T1>,
		T2 extends TypeLattice<T2>> extends TypeCartesianCombination<TypeLatticeProduct<T1, T2>, T1, T2> {

	/**
	 * Builds a new product of two lattices.
	 * 
	 * @param first  the first lattice
	 * @param second the second lattice
	 */
	public TypeLatticeProduct(
			T1 first,
			T2 second) {
		super(first, second);
	}

	@Override
	public TypeLatticeProduct<T1, T2> mk(
			T1 first,
			T2 second) {
		return new TypeLatticeProduct<>(first, second);
	}

	@Override
	public TypeLatticeProduct<T1, T2> store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return new TypeLatticeProduct<>(first.store(target, source), second.store(target, source));
	}

}
