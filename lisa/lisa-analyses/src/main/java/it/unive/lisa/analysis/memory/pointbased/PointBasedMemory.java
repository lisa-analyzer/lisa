package it.unive.lisa.analysis.memory.pointbased;

import it.unive.lisa.analysis.nonrelational.memory.MemoryEnvironment;
import it.unive.lisa.lattices.memory.allocations.AllocationSites;

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
public class PointBasedMemory
		extends
		AllocationSiteBasedAnalysis<MemoryEnvironment<AllocationSites>> {

	@Override
	public MemoryEnvironment<AllocationSites> makeLattice() {
		return new MemoryEnvironment<>(new AllocationSites());
	}

}
