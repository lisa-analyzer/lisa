package it.unive.lisa.lattices.heap.allocations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class StackAllocationSiteTest {

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	@Test
	public void nameEncodesLocationAndField() {
		StackAllocationSite noField = new StackAllocationSite(Untyped.INSTANCE, "l", false, loc);
		assertEquals("pp@l", noField.getName());

		StackAllocationSite withField = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		assertEquals("pp@l[f]", withField.getName());
	}

	@Test
	public void fieldConstructorFromSymbolicExpressionUsesItsToString() {
		StackAllocationSite site = new StackAllocationSite(
				Untyped.INSTANCE, "l", new Variable(Untyped.INSTANCE, "f", loc), false, loc);
		assertEquals("f", site.getField());
	}

	@Test
	public void twoSitesWithSameNameAndWeaknessAreEqualRegardlessOfType() {
		StackAllocationSite a = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		StackAllocationSite b = new StackAllocationSite(
				it.unive.lisa.program.type.Int32Type.INSTANCE, "l", "f", false, loc);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void aStackAllocationSiteIsNeverEqualToAHeapAllocationSiteWithTheSameNameAndWeakness() {
		StackAllocationSite stack = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite heap = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		// stack allocations use shallow-copy (value) semantics on assignment
		// while heap allocations use aliasing (reference) semantics: the two
		// kinds must never be conflated by equals, even with an identical name
		assertNotEquals(stack, heap);
	}

	@Test
	public void toWeakOfAStrongSiteProducesAWeakStackAllocationSite() {
		StackAllocationSite strong = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		StackAllocationSite weak = strong.toWeak();
		assertTrue(weak.isWeak());
		assertEquals(strong.getLocationName(), weak.getLocationName());
		assertEquals(strong.getField(), weak.getField());
	}

	@Test
	public void withFieldAndWithoutFieldRoundTripOnAStackAllocationSite() {
		StackAllocationSite site = new StackAllocationSite(Untyped.INSTANCE, "l", false, loc);
		StackAllocationSite withField = site.withField(new Variable(Untyped.INSTANCE, "f", loc));
		assertEquals("f", withField.getField());
		StackAllocationSite backToNoField = withField.withoutField();
		assertNull(backToNoField.getField());
		assertEquals(site, backToNoField);
	}

	@Test
	public void withFieldOnAnAlreadyFieldedStackSiteThrows() {
		StackAllocationSite site = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		assertThrows(IllegalStateException.class, () -> site.withField(new Variable(Untyped.INSTANCE, "g", loc)));
	}

	@Test
	public void asNonAllocationPreservesTheFieldSinceStackSitesCarryItInEveryConstructor() {
		StackAllocationSite withField = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		withField.setAllocation(true);
		StackAllocationSite result = withField.asNonAllocation();
		assertEquals("f", result.getField());
		assertEquals(withField, result);
	}

	@Test
	public void toStringReflectsWeaknessAndName() {
		StackAllocationSite strong = new StackAllocationSite(Untyped.INSTANCE, "l", false, loc);
		StackAllocationSite weak = new StackAllocationSite(Untyped.INSTANCE, "l", true, loc);
		assertEquals("heap[s]:pp@l", strong.toString());
		assertEquals("heap[w]:pp@l", weak.toString());
	}

	@Test
	public void lubOfAStrongAndAWeakSiteWithTheSameNameIsTheWeakOne()
			throws SemanticException {
		StackAllocationSite strong = new StackAllocationSite(Untyped.INSTANCE, "l", false, loc);
		StackAllocationSite weak = new StackAllocationSite(Untyped.INSTANCE, "l", true, loc);
		assertEquals(weak, strong.lub(weak));
		assertEquals(weak, weak.lub(strong));
	}

	@Test
	public void lubOfSitesWithDifferentNamesThrows() {
		StackAllocationSite a = new StackAllocationSite(Untyped.INSTANCE, "l", false, loc);
		StackAllocationSite b = new StackAllocationSite(Untyped.INSTANCE, "m", false, loc);
		assertThrows(SemanticException.class, () -> a.lub(b));
	}

}
