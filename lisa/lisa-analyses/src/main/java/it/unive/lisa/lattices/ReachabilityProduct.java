package it.unive.lisa.lattices;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.combination.AbstractLatticeProduct;

public class ReachabilityProduct<A extends AbstractLattice<A>>
		extends
		AbstractLatticeProduct<ReachabilityProduct<A>, ReachLattice, A> {

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

	public ReachabilityProduct<A> setToReachable() {
		ReachLattice reach = first.setToReachable();
		if (reach == first)
			return this;
		return new ReachabilityProduct<>(reach, second);
	}

	public ReachabilityProduct<A> setToUnreachable() {
		ReachLattice reach = first.setToUnreachable();
		if (reach == first)
			return this;
		return new ReachabilityProduct<>(reach, second);
	}

	public ReachabilityProduct<A> setToPossiblyReachable() {
		ReachLattice reach = first.setToPossiblyReachable();
		if (reach == first)
			return this;
		return new ReachabilityProduct<>(reach, second);
	}

}