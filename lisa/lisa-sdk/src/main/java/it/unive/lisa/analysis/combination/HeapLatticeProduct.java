package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link HeapCartesianCombination} of two arbitrary {@link HeapLattice}s,
 * with no reduction.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T1> the type of the first lattice
 * @param <T2> the type of the second lattice
 */
public class HeapLatticeProduct<T1 extends HeapLattice<T1>,
		T2 extends HeapLattice<T2>>
		extends
		HeapCartesianCombination<HeapLatticeProduct<T1, T2>, T1, T2> {

	/**
	 * Builds a new product of two lattices.
	 * 
	 * @param first  the first lattice
	 * @param second the second lattice
	 */
	public HeapLatticeProduct(
			T1 first,
			T2 second) {
		super(first, second);
	}

	@Override
	public HeapLatticeProduct<T1, T2> mk(
			T1 first,
			T2 second) {
		return new HeapLatticeProduct<>(first, second);
	}

	@Override
	protected Pair<HeapLatticeProduct<T1, T2>, List<HeapReplacement>> mk(
			Pair<T1, List<HeapReplacement>> first,
			Pair<T2, List<HeapReplacement>> second) {
		List<HeapReplacement> replacements = new LinkedList<>();
		replacements.addAll(first.getRight());
		replacements.addAll(second.getRight());
		return Pair
				.of(
						new HeapLatticeProduct<>(first.getLeft(), second.getLeft()),
						replacements);
	}

	@Override
	public List<HeapReplacement> expand(
			HeapReplacement base)
			throws SemanticException {
		List<HeapReplacement> result = new LinkedList<>();
		result.addAll(first.expand(base));
		result.addAll(second.expand(base));
		return result;
	}

}
