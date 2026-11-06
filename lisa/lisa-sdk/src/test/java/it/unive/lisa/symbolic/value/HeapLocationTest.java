package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class HeapLocationTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void equalsRequiresSameNameAndSameWeaknessButIgnoresTypeAndAllocation() {
		HeapLocation a = new HeapLocation(Untyped.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		HeapLocation b = new HeapLocation(VoidType.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		b.setAllocation(true);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		HeapLocation differentWeak = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		assertFalse(a.equals(differentWeak));

		HeapLocation differentName = new HeapLocation(Untyped.INSTANCE, "other", true, SyntheticLocation.INSTANCE);
		assertFalse(a.equals(differentName));
	}

	@Test
	public void toStringEncodesWeaknessAndName() {
		HeapLocation weak = new HeapLocation(Untyped.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		HeapLocation strong = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		assertEquals("heap[w]:loc", weak.toString());
		assertEquals("heap[s]:loc", strong.toString());
	}

	@Test
	public void lubOfSameNamePrefersTheWeakOneRegardlessOfCallOrder() throws SemanticException {
		// weak is the coarser (more approximated) of the two, so the lub of a
		// weak and a strong location with the same name is always the weak
		// one, whichever side the call is invoked on
		HeapLocation weak = new HeapLocation(Untyped.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		HeapLocation strong = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);

		assertSame(weak, weak.lub(strong));
		assertSame(weak, strong.lub(weak));
	}

	@Test
	public void lubOfDifferentNamesThrows() {
		HeapLocation a = new HeapLocation(Untyped.INSTANCE, "a", false, SyntheticLocation.INSTANCE);
		HeapLocation b = new HeapLocation(Untyped.INSTANCE, "b", false, SyntheticLocation.INSTANCE);
		assertThrows(SemanticException.class, () -> a.lub(b));
	}

	@Test
	public void canBeScopedIsFalseAndPushPopAreNoOps() throws Exception {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		assertFalse(loc.canBeScoped());
		assertSame(loc, loc.pushScope(TOKEN, null));
		assertSame(loc, loc.popScope(TOKEN, null));
	}

	@Test
	public void isAllocationDefaultsToFalseAndCanBeSet() {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		assertFalse(loc.isAllocation());
		loc.setAllocation(true);
		assertTrue(loc.isAllocation());
	}

	@Test
	public void asNonAllocationIsIdentityWhenNotAnAllocation() {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", false, SyntheticLocation.INSTANCE);
		assertSame(loc, loc.asNonAllocation());
	}

	@Test
	public void asNonAllocationRebuildsAnEquivalentNonAllocationLocation() {
		HeapLocation loc = new HeapLocation(Untyped.INSTANCE, "loc", true, SyntheticLocation.INSTANCE);
		loc.setAllocation(true);
		HeapLocation nonAlloc = loc.asNonAllocation();
		assertFalse(nonAlloc.isAllocation());
		assertEquals(loc, nonAlloc);
		assertEquals(loc.getName(), nonAlloc.getName());
		assertEquals(loc.isWeak(), nonAlloc.isWeak());
	}

}
