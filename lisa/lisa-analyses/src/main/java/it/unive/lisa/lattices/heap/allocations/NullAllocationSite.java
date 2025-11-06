package it.unive.lisa.lattices.heap.allocations;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.NullType;

/**
 * A singleton allocation site representing a uninitialized/null location.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NullAllocationSite
		extends
		HeapAllocationSite {

	/**
	 * The singleton instance of this allocation site.
	 */
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
