package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;

/**
 * A field-insensitive, allocation-site-based heap domain: every field access on
 * a given allocation site is abstracted away, meaning that heap locations only
 * distinguish objects and arrays by their allocation site, and not by the
 * fields or elements stored inside them. The implementation follows X. Rival
 * and K. Yi, "Introduction to Static Analysis: An Abstract Interpretation
 * Perspective", Section 8.3.4.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 *
 * @see <a href="https://mitpress.mit.edu/books/introduction-static-analysis">
 *          Xavier Rival, Kwangkeun Yi. Introduction to Static Analysis: An
 *          Abstract Interpretation Perspective. MIT Press, 2020.</a>
 */
public class PointBasedHeap
		extends
		AllocationSiteBasedAnalysis<HeapEnvironment<AllocationSites>> {

	@Override
	public HeapEnvironment<AllocationSites> makeLattice() {
		return new HeapEnvironment<>(new AllocationSites());
	}

}
