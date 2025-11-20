package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;

/**
 * A field-insensitive program point-based {@link AllocationSiteBasedAnalysis}.
 * The implementation follows X. Rival and K. Yi, "Introduction to Static
 * Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class PointBasedHeap
		extends
		AllocationSiteBasedAnalysis<HeapEnvironment<AllocationSites>> {

	@Override
	public HeapEnvironment<AllocationSites> makeLattice() {
		return new HeapEnvironment<>(new AllocationSites());
	}

}
