package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NullTypeTest {

	// ReferenceType is a PointerType, not an InMemoryType (they are disjoint
	// categories - see Type#isValueType()), so a dedicated fake is needed to
	// exercise the "assignable to any in-memory type" branch
	private static class FakeInMemoryType
			implements
			InMemoryType {

		// AccessModifiers#testVisibilityOfTypes requires every concrete
		// Type to expose a public/protected no-arg constructor
		public FakeInMemoryType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return equals(other);
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}
	}

	@Test
	public void toStringIsNull() {
		assertEquals("null", NullType.INSTANCE.toString());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsAcceptsAnyNullTypeInstance() {
		assertTrue(NullType.INSTANCE.equals(NullType.INSTANCE));
		assertFalse(NullType.INSTANCE.equals(null));
		assertFalse(NullType.INSTANCE.equals(VoidType.INSTANCE));
		assertEquals(NullType.class.hashCode(), NullType.INSTANCE.hashCode());
	}

	@Test
	public void canBeAssignedToAnyInMemoryTypeOrUntyped() {
		// null can be the value of any in-memory-typed variable (units,
		// arrays...) as well as any untyped one; note that ReferenceType is
		// a PointerType rather than an InMemoryType, so it is NOT one of the
		// types null can be assigned to
		assertTrue(NullType.INSTANCE.canBeAssignedTo(NullType.INSTANCE));
		assertTrue(NullType.INSTANCE.canBeAssignedTo(new FakeInMemoryType()));
		assertTrue(NullType.INSTANCE.canBeAssignedTo(Untyped.INSTANCE));
		assertFalse(NullType.INSTANCE.canBeAssignedTo(VoidType.INSTANCE));
		assertFalse(NullType.INSTANCE.canBeAssignedTo(new ReferenceType(Untyped.INSTANCE)));
	}

	@Test
	public void commonSupertypeWithAnInMemoryTypeIsThatType() {
		FakeInMemoryType other = new FakeInMemoryType();
		assertSame(other, NullType.INSTANCE.commonSupertype(other));
	}

	@Test
	public void commonSupertypeWithANonInMemoryTypeOrNullIsUntyped() {
		assertSame(Untyped.INSTANCE, NullType.INSTANCE.commonSupertype(VoidType.INSTANCE));
		assertSame(Untyped.INSTANCE, NullType.INSTANCE.commonSupertype(null));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(NullType.INSTANCE), NullType.INSTANCE.allInstances(new MinimalTypeSystem()));
	}

}
