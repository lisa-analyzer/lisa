package it.unive.lisa.lattices.heap.allocations;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapValue;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A heap lattice tracking sets of {@link AllocationSite}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class AllocationSites
		extends
		SetLattice<AllocationSites, AllocationSite>
		implements
		HeapValue<AllocationSites> {

	private static final AllocationSites TOP = new AllocationSites(new HashSet<>(), true);

	private static final AllocationSites BOTTOM = new AllocationSites(new HashSet<>(), false);

	/**
	 * Builds an instance of HeapIdentiferSetLattice, corresponding to the top
	 * element.
	 */
	public AllocationSites() {
		this(new HashSet<>(), true);
	}

	/**
	 * Builds an instance of this class to hold the given sites.
	 * 
	 * @param set the set of {@link AllocationSite}s
	 */
	public AllocationSites(
			Set<AllocationSite> set) {
		super(set, true);
	}

	private AllocationSites(
			Set<AllocationSite> set,
			boolean isTop) {
		super(set, isTop);
	}

	/**
	 * Builds an instance of this class to hold the given site.
	 * 
	 * @param site the {@link AllocationSite} to hold
	 */
	public AllocationSites(
			AllocationSite site) {
		this(Collections.singleton(site), true);
	}

	@Override
	public AllocationSites top() {
		return TOP;
	}

	@Override
	public AllocationSites bottom() {
		return BOTTOM;
	}

	@Override
	public AllocationSites mk(
			Set<AllocationSite> set) {
		return new AllocationSites(set, true);
	}

	@Override
	public Iterator<AllocationSite> iterator() {
		return this.elements.iterator();
	}

	@Override
	public boolean lessOrEqualAux(
			AllocationSites other)
			throws SemanticException {
		Set<AllocationSite> hullLeft = new HashSet<>(elements);
		Set<AllocationSite> hullRight = new HashSet<>(other.elements);
		hullLeft.removeAll(other.elements);
		hullRight.removeAll(elements);
		if (hullLeft.isEmpty() && hullRight.isEmpty())
			return true;

		// there could be strong identifiers in this that are
		// replaced by weak identifiers in other
		for (AllocationSite left : hullLeft)
			if (left.isWeak())
				// a weak identifier cannot be replaced by another one
				return false;
			else {
				// if the left is strong, it must be present in the right
				// as a weak identifier
				Optional<AllocationSite> match = hullRight
						.stream()
						.filter(e -> e.getName().equals(left.getName()) && e.isWeak())
						.findAny();
				if (match.isEmpty())
					// if there is no match, then the partial order does not
					// hold
					return false;
				else
					// if there is a match, we just remove the alternative for
					// other identifiers
					// and proceed with the next one
					hullRight.remove(match.get());
			}

		// if we reach here we found a match for all strong identifiers, we can
		// just return true
		return true;
	}

	@Override
	public AllocationSites lubAux(
			AllocationSites other)
			throws SemanticException {
		Map<String, AllocationSite> lub = new HashMap<>();

		// all weak identifiers are part of the lub
		elements.stream().filter(AllocationSite::isWeak).forEach(e -> lub.put(e.getName(), e));
		// common ones will be overwritten
		other.elements.stream().filter(AllocationSite::isWeak).forEach(e -> lub.put(e.getName(), e));

		// strong identifiers are only added if we did not consider a
		// weak identifier with the same name
		elements
				.stream()
				.filter(Predicate.not(AllocationSite::isWeak))
				.filter(e -> !lub.containsKey(e.getName()))
				.forEach(e -> lub.put(e.getName(), e));

		other.elements
				.stream()
				.filter(Predicate.not(AllocationSite::isWeak))
				.filter(e -> !lub.containsKey(e.getName()))
				.forEach(e -> lub.put(e.getName(), e));

		return new AllocationSites(new HashSet<>(lub.values()), true);
	}

	/**
	 * Applies a substitution to the contents of this set of allocation sites,
	 * similarly to
	 * {@link ValueDomain#applyReplacement(it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement, ProgramPoint, SemanticOracle)}.
	 * 
	 * @param r  the substitution
	 * @param pp the program point where the substitution is applied
	 * 
	 * @return a copy of this set of sites where the substitution has been
	 *             applied
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AllocationSites applyReplacement(
			HeapReplacement r,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop() || isBottom() || r.getSources().isEmpty())
			return this;

		Set<AllocationSite> copy = new HashSet<>(elements);
		if (copy.removeAll(r.getSources())) {
			r
					.getTargets()
					.stream()
					.filter(AllocationSite.class::isInstance)
					.map(AllocationSite.class::cast)
					.forEach(copy::add);
			return new AllocationSites(copy, isTop);
		} else
			return this;
	}

	@Override
	public AllocationSites unknownValue(
			Identifier id) {
		// we use bottom since heap environments track all possible keys: the
		// absence of a key means no information (bottom) instead of any
		// possible information (top)
		return bottom();
	}

	/**
	 * Returns the set of allocation sites that can be reached <b>only</b> from
	 * the given identifier, and through no other path in the memory. This
	 * method assumes that the points-to information is available in the given
	 * state in the form of a map from identifiers to sets of allocation sites.
	 *
	 * @param <F>   the type of the mapping for the points-to information
	 * @param state the state containing the points-to information
	 * @param id    the identifier for which to compute the reachable allocation
	 *                  sites
	 * 
	 * @return the set of allocation sites reachable only from the given
	 *             identifier
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	@Override
	public <F extends FunctionalLattice<F, Identifier, AllocationSites>> Collection<Identifier> reachableOnlyFrom(
			F state,
			Identifier id)
			throws SemanticException {
		// this gives us a quick way of checking if an id can be reached
		// through multiple ancestors
		Map<Identifier, Set<Identifier>> pointedTo = new HashMap<>();
		for (Entry<Identifier, AllocationSites> entry : state) {
			Identifier key = entry.getKey();
			if (key instanceof AllocationSite)
				// field information is not relevant for the cut
				key = ((AllocationSite) key).withoutField();
			for (AllocationSite site : entry.getValue())
				// field information is not relevant for the cut
				pointedTo.computeIfAbsent(site.withoutField(), k -> new HashSet<>()).add(key);
		}

		Set<Identifier> reachable = new HashSet<>();
		Set<Identifier> frontier = new HashSet<>();

		reachable.add(id);
		frontier.add(id);

		do {
			Set<Identifier> tmp = new HashSet<>();
			for (Identifier current : frontier)
				for (AllocationSite site : state.getState(current)) {
					site = site.withoutField();
					if (!reachable.contains(site) && pointedTo.get(site).size() == 1) {
						reachable.add(site);
						tmp.add(site);
					}
				}
			frontier = tmp;
		} while (!frontier.isEmpty());

		return reachable;
	}

}
