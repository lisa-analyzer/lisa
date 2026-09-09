package it.unive.lisa.lattices.heap.allocations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AllocationSitesTest {

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	private final HeapAllocationSite strongA = new HeapAllocationSite(Untyped.INSTANCE, "a", false, loc);

	private final HeapAllocationSite weakA = new HeapAllocationSite(Untyped.INSTANCE, "a", true, loc);

	private final HeapAllocationSite strongB = new HeapAllocationSite(Untyped.INSTANCE, "b", false, loc);

	private final HeapAllocationSite weakB = new HeapAllocationSite(Untyped.INSTANCE, "b", true, loc);

	@Test
	public void aStrongSiteIsLessOrEqualThanAWeakSiteWithTheSameName()
			throws SemanticException {
		AllocationSites strong = new AllocationSites(Set.of(strongA));
		AllocationSites weak = new AllocationSites(Set.of(weakA));
		assertTrue(strong.lessOrEqual(weak));
		assertFalse(weak.lessOrEqual(strong));
	}

	@Test
	public void aWeakSiteIsNotLessOrEqualThanAStrongOneWithTheSameName()
			throws SemanticException {
		AllocationSites strong = new AllocationSites(Set.of(strongA));
		AllocationSites weak = new AllocationSites(Set.of(weakA));
		assertFalse(weak.lessOrEqual(strong));
	}

	@Test
	public void aSiteWithNoCounterpartByNameIsNotLessOrEqual()
			throws SemanticException {
		AllocationSites a = new AllocationSites(Set.of(strongA));
		AllocationSites b = new AllocationSites(Set.of(strongB));
		assertFalse(a.lessOrEqual(b));
	}

	@Test
	public void anIdenticalSetIsLessOrEqualToItself()
			throws SemanticException {
		AllocationSites set = new AllocationSites(Set.of(strongA, weakB));
		assertTrue(set.lessOrEqual(new AllocationSites(Set.of(strongA, weakB))));
	}

	@Test
	public void lubKeepsTheWeakVariantWhenBothAreProvided()
			throws SemanticException {
		AllocationSites strong = new AllocationSites(Set.of(strongA));
		AllocationSites weak = new AllocationSites(Set.of(weakA));
		AllocationSites lub = strong.lub(weak);
		assertEquals(Set.of(weakA), lub.elements());
	}

	@Test
	public void lubKeepsAStrongSiteWhenNoWeakCounterpartExists()
			throws SemanticException {
		AllocationSites a = new AllocationSites(Set.of(strongA));
		AllocationSites b = new AllocationSites(Set.of(strongB));
		AllocationSites lub = a.lub(b);
		assertEquals(Set.of(strongA, strongB), lub.elements());
	}

	@Test
	public void unknownValueIsBottomNotTop() {
		AllocationSites sites = new AllocationSites();
		assertTrue(sites.unknownValue(null).isBottom());
	}

}
