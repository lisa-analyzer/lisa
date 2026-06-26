package it.unive.lisa.analysis.combination;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.analysis.memory.MemoryLattice;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link MemoryCartesianCombination} of two arbitrary {@link MemoryLattice}s,
 * with no reduction.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <T1> the type of the first lattice
 * @param <T2> the type of the second lattice
 */
public class MemoryLatticeProduct<T1 extends MemoryLattice<T1>,
		T2 extends MemoryLattice<T2>>
		extends
		MemoryCartesianCombination<MemoryLatticeProduct<T1, T2>, T1, T2> {

	/**
	 * Builds a new product of two lattices.
	 * 
	 * @param first  the first lattice
	 * @param second the second lattice
	 */
	public MemoryLatticeProduct(
			T1 first,
			T2 second) {
		super(first, second);
	}

	@Override
	public MemoryLatticeProduct<T1, T2> mk(
			T1 first,
			T2 second) {
		return new MemoryLatticeProduct<>(first, second);
	}

	@Override
	protected Pair<MemoryLatticeProduct<T1, T2>, List<MemoryReplacement>> mk(
			Pair<T1, List<MemoryReplacement>> first,
			Pair<T2, List<MemoryReplacement>> second) {
		List<MemoryReplacement> replacements = new LinkedList<>();
		replacements.addAll(first.getRight());
		replacements.addAll(second.getRight());
		return Pair.of(new MemoryLatticeProduct<>(first.getLeft(), second.getLeft()), replacements);
	}

	@Override
	public List<MemoryReplacement> expand(
			MemoryReplacement base)
			throws SemanticException {
		List<MemoryReplacement> result = new LinkedList<>();
		result.addAll(first.expand(base));
		result.addAll(second.expand(base));
		return result;
	}

}
