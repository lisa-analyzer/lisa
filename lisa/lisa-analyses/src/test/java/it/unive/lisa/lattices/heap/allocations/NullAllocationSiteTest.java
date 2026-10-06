package it.unive.lisa.lattices.heap.allocations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.type.NullType;
import org.junit.jupiter.api.Test;

public class NullAllocationSiteTest {

	@Test
	public void instanceIsASingleton() {
		assertSame(NullAllocationSite.INSTANCE, NullAllocationSite.INSTANCE);
	}

	@Test
	public void cannotBeAssigned() {
		assertFalse(NullAllocationSite.INSTANCE.canBeAssigned());
	}

	@Test
	public void printsAsNull() {
		assertEquals("null", NullAllocationSite.INSTANCE.toString());
	}

	@Test
	public void hasNullType() {
		assertEquals(NullType.INSTANCE, NullAllocationSite.INSTANCE.getStaticType());
	}

	@Test
	public void isNotWeak() {
		assertFalse(NullAllocationSite.INSTANCE.isWeak());
	}

}
