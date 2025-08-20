package it.unive.lisa.lattices.numeric;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.ValueCartesianCombination;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

/**
 * The lattice structure of the pentagons analysis, which is a cartesian product
 * of two lattices: a map from identifiers to {@link IntInterval}s, and a map
 * from identifiers to {@link DefiniteIdSet}s (representing upper bounds
 * information). This class also implements the necessary reductions to refine
 * the bounds based on the intervals.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class PentagonLattice
		extends
		ValueCartesianCombination<PentagonLattice, ValueEnvironment<IntInterval>, ValueEnvironment<DefiniteIdSet>> {

	/**
	 * Builds a new reduced product between intervals and upper bounds.
	 */
	public PentagonLattice() {
		super(
				new ValueEnvironment<>(IntInterval.TOP),
				new ValueEnvironment<>(new DefiniteIdSet(Collections.emptySet())));
	}

	/**
	 * Builds a new reduced product between the given intervals and upper
	 * bounds.
	 * 
	 * @param first  the intervals
	 * @param second the upper bounds
	 */
	public PentagonLattice(
			ValueEnvironment<IntInterval> first,
			ValueEnvironment<DefiniteIdSet> second) {
		super(first, second);
	}

	@Override
	public PentagonLattice mk(
			ValueEnvironment<IntInterval> first,
			ValueEnvironment<DefiniteIdSet> second) {
		return new PentagonLattice(first, second);
	}

	@Override
	public boolean lessOrEqualAux(
			PentagonLattice other)
			throws SemanticException {
		if (!first.lessOrEqual(other.first))
			return false;

		for (Entry<Identifier, DefiniteIdSet> entry : other.second)
			for (Identifier bound : entry.getValue()) {
				if (second.getState(entry.getKey()).contains(bound))
					continue;

				IntInterval state = first.getState(entry.getKey());
				IntInterval boundState = first.getState(bound);
				if (state.isBottom() || boundState.isTop() || state.getHigh().compareTo(boundState.getLow()) < 0)
					continue;

				return false;
			}

		return true;
	}

	@Override
	public PentagonLattice lubAux(
			PentagonLattice other)
			throws SemanticException {
		ValueEnvironment<IntInterval> newIntervals = first.lub(other.first);

		// lub performs the intersection between the two
		// this effectively builds s'
		ValueEnvironment<DefiniteIdSet> newBounds = second.lub(other.second);

		// the following builds s''
		for (Identifier x : second.getKeys()) {
			DefiniteIdSet closure = newBounds.getState(x);

			IntInterval b2_x = other.first.getState(x);
			if (!b2_x.isBottom()) {
				for (Identifier y : second.getState(x)) {
					IntInterval b2_y = other.first.getState(y);
					if (!b2_y.isBottom() && b2_x.getHigh().compareTo(b2_y.getLow()) < 0) {
						closure = closure.add(y);
					}
				}
			}

			newBounds = newBounds.putState(x, closure);
		}

		// the following builds s'''
		for (Identifier x : other.second.getKeys()) {
			DefiniteIdSet closure = newBounds.getState(x);

			IntInterval b1_x = first.getState(x);
			if (!b1_x.isBottom())
				for (Identifier y : other.second.getState(x)) {
					IntInterval b1_y = first.getState(y);
					if (!b1_y.isBottom() && b1_x.getHigh().compareTo(b1_y.getLow()) < 0)
						closure = closure.add(y);
				}

			newBounds = newBounds.putState(x, closure);
		}

		return new PentagonLattice(newIntervals, newBounds);
	}

	/**
	 * Performs a reduction of the current state, refining the upper bounds
	 * based on the intervals. This is a closure operation that ensures that the
	 * upper bounds are consistent with the intervals.
	 * 
	 * @return the reduced state
	 * 
	 * @throws SemanticException if an error occurs during the reduction
	 */
	public PentagonLattice closure()
			throws SemanticException {
		ValueEnvironment<DefiniteIdSet> newBounds = new ValueEnvironment<>(second.lattice, second.getMap());

		for (Identifier id1 : first.getKeys()) {
			Set<Identifier> closure = new HashSet<>();
			for (Identifier id2 : first.getKeys())
				if (!id1.equals(id2))
					if (first.getState(id1).getHigh().compareTo(first.getState(id2).getLow()) < 0)
						closure.add(id2);
			if (!closure.isEmpty())
				// glb is the union
				newBounds = newBounds.putState(id1, newBounds.getState(id1).glb(new DefiniteIdSet(closure)));

		}

		return new PentagonLattice(first, newBounds);
	}

	@Override
	public PentagonLattice store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return new PentagonLattice(first.store(target, source), second.store(target, source));
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		for (Identifier id : CollectionUtils.union(first.getKeys(), second.getKeys()))
			mapping.put(
					new StringRepresentation(id),
					new StringRepresentation(
							first.getState(id).toString() + ", " + second.getState(id).representation()));
		return new MapRepresentation(mapping);
	}

}