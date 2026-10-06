package it.unive.lisa.lattices.heap.allocations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class AllocationSiteTest {

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	@Test
	public void locationNameAndFieldAreExposedAsGiven() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		assertEquals("l", site.getLocationName());
		assertEquals("f", site.getField());
	}

	@Test
	public void fieldIsNullWhenNotSpecified() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		assertNull(site.getField());
	}

	@Test
	public void toWeakOfAStrongSiteIsWeakAndOtherwiseEqual() {
		HeapAllocationSite strong = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite weak = strong.toWeak();
		assertFalse(strong.isWeak());
		assertTrue(weak.isWeak());
		assertEquals(strong.getLocationName(), weak.getLocationName());
		assertEquals(strong.getField(), weak.getField());
	}

	@Test
	public void toWeakOfAWeakSiteReturnsTheSameInstance() {
		HeapAllocationSite weak = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", true, loc);
		assertEquals(weak, weak.toWeak());
	}

	@Test
	public void withFieldAddsAFieldToAFieldlessSite() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		HeapAllocationSite withField = site.withField(new it.unive.lisa.symbolic.value.Variable(
				Untyped.INSTANCE, "f", loc));
		assertEquals("f", withField.getField());
	}

	@Test
	public void withFieldOnAnAlreadyFieldedSiteThrows() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		assertThrows(
				IllegalStateException.class,
				() -> site.withField(
						new it.unive.lisa.symbolic.value.Variable(Untyped.INSTANCE, "g", loc)));
	}

	@Test
	public void withoutFieldRemovesTheField() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite withoutField = site.withoutField();
		assertNull(withoutField.getField());
		assertEquals(site.getLocationName(), withoutField.getLocationName());
	}

	@Test
	public void withoutFieldOnAFieldlessSiteReturnsTheSameInstance() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		assertEquals(site, site.withoutField());
	}

	@Test
	public void withTypeChangesOnlyTheType() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite retyped = site.withType(it.unive.lisa.program.type.Int32Type.INSTANCE);
		assertEquals(it.unive.lisa.program.type.Int32Type.INSTANCE, retyped.getStaticType());
		assertEquals(site.getLocationName(), retyped.getLocationName());
		assertEquals(site.getField(), retyped.getField());
	}

	// asNonAllocation() is documented (see HeapLocation#asNonAllocation) to
	// yield "a version where isAllocation() returns false", i.e. it must only
	// flip the isAllocation flag and otherwise preserve identity - equals()
	// and hashCode() deliberately ignore isAllocation for this reason. The
	// current HeapAllocationSite#asNonAllocation implementation instead
	// rebuilds the site with the fieldless constructor, silently dropping
	// the field and thus changing the site's name/identity when a field is
	// present. StackAllocationSite#asNonAllocation correctly preserves the
	// field, which is why this test is expected to fail only for
	// HeapAllocationSite.
	@Test
	public void asNonAllocationPreservesTheFieldOnHeapAllocationSites() {
		HeapAllocationSite withField = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		withField.setAllocation(true);
		HeapAllocationSite result = withField.asNonAllocation();
		assertFalse(result.isAllocation());
		assertEquals(withField.getField(), result.getField());
		assertEquals(withField, result);
	}

	@Test
	public void asNonAllocationPreservesTheFieldOnStackAllocationSites() {
		StackAllocationSite withField = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		withField.setAllocation(true);
		StackAllocationSite result = withField.asNonAllocation();
		assertFalse(result.isAllocation());
		assertEquals(withField.getField(), result.getField());
		assertEquals(withField, result);
	}

	@Test
	public void asNonAllocationOnANonAllocationReturnsTheSameInstance() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		assertFalse(site.isAllocation());
		assertEquals(site, site.asNonAllocation());
	}

}
