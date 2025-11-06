package it.unive.lisa.lattices.heap.allocations;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.NullType;

public class NullAllocationSite
		extends
		HeapAllocationSite {

	public static NullAllocationSite INSTANCE = new NullAllocationSite();

	private NullAllocationSite() {
		super(NullType.INSTANCE, "null", false, SyntheticLocation.INSTANCE);
	}

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public boolean canBeAssigned() {
		return false;
	}
}
