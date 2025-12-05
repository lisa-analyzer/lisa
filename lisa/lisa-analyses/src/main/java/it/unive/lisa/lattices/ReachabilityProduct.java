package it.unive.lisa.lattices;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.combination.AbstractLatticeProduct;

/**
 * A product between an {@link AbstractLattice}, tracking arbitrary information,
 * and a {@link ReachLattice}, tracking the reachability of a program point.
 * This lattice can be used to modularly enrich any analysis with reachability
 * information, allowing for customization of warnings to be issued.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of the state being tracked
 */
public class ReachabilityProduct<A extends AbstractLattice<A>>
		extends
		AbstractLatticeProduct<ReachabilityProduct<A>, ReachLattice, A> {

	/**
	 * Builds the reachability product.
	 *
	 * @param first  the reachability lattice
	 * @param second the state being tracked
	 */
	public ReachabilityProduct(
			ReachLattice first,
			A second) {
		super(first, second);
	}

	@Override
	public ReachabilityProduct<A> withTopMemory() {
		A top = second.withTopMemory();
		if (top == second)
			return this;
		return new ReachabilityProduct<>(first, top);
	}

	@Override
	public ReachabilityProduct<A> withTopValues() {
		A top = second.withTopValues();
		if (top == second)
			return this;
		return new ReachabilityProduct<>(first, top);
	}

	@Override
	public ReachabilityProduct<A> withTopTypes() {
		A top = second.withTopTypes();
		if (top == second)
			return this;
		return new ReachabilityProduct<>(first, top);
	}

	@Override
	public ReachabilityProduct<A> mk(
			ReachLattice first,
			A second) {
		if (second.isBottom())
			// reduction: bottom turns the reachability to unreachable
			return new ReachabilityProduct<>(first.setToUnreachable(), second);
		return new ReachabilityProduct<>(first, second);
	}

	/**
	 * Yields a copy of this product, modified to have its reachability set to
	 * unreachable.
	 * 
	 * @return the modified copy
	 */
	public ReachabilityProduct<A> setToReachable() {
		ReachLattice reach = first.setToReachable();
		if (reach == first)
			return this;
		return new ReachabilityProduct<>(reach, second);
	}

	/**
	 * Yields a copy of this product, modified to have its reachability set to
	 * unreachable.
	 * 
	 * @return the modified copy
	 */
	public ReachabilityProduct<A> setToUnreachable() {
		ReachLattice reach = first.setToUnreachable();
		if (reach == first)
			return this;
		return new ReachabilityProduct<>(reach, second);
	}

	/**
	 * Yields a copy of this product, modified to have its reachability set to
	 * possibly reachable.
	 * 
	 * @return the modified copy
	 */
	public ReachabilityProduct<A> setToPossiblyReachable() {
		ReachLattice reach = first.setToPossiblyReachable();
		if (reach == first)
			return this;
		return new ReachabilityProduct<>(reach, second);
	}

}
