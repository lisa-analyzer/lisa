package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class UntypedTest {

	private static class OtherType
			implements
			Type {

		// AccessModifiers#testVisibilityOfTypes requires every concrete
		// Type to expose a public/protected no-arg constructor
		public OtherType() {
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
	public void toStringIsUntyped() {
		assertEquals("untyped", Untyped.INSTANCE.toString());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsAcceptsAnyUntypedInstance() {
		assertTrue(Untyped.INSTANCE.equals(Untyped.INSTANCE));
		assertFalse(Untyped.INSTANCE.equals(null));
		assertFalse(Untyped.INSTANCE.equals(new OtherType()));
		assertEquals(Untyped.class.hashCode(), Untyped.INSTANCE.hashCode());
	}

	@Test
	public void canBeAssignedToIsTrueOnlyForUntyped() {
		assertTrue(Untyped.INSTANCE.canBeAssignedTo(Untyped.INSTANCE));
		assertFalse(Untyped.INSTANCE.canBeAssignedTo(VoidType.INSTANCE));
		assertFalse(Untyped.INSTANCE.canBeAssignedTo(new OtherType()));
	}

	@Test
	public void commonSupertypeAlwaysReturnsItself() {
		assertSame(Untyped.INSTANCE, Untyped.INSTANCE.commonSupertype(VoidType.INSTANCE));
		assertSame(Untyped.INSTANCE, Untyped.INSTANCE.commonSupertype(new OtherType()));
		assertSame(Untyped.INSTANCE, Untyped.INSTANCE.commonSupertype(Untyped.INSTANCE));
	}

	@Test
	public void allInstancesDelegatesToTheTypeSystem() {
		MinimalTypeSystem ts = new MinimalTypeSystem();
		ts.registerType(VoidType.INSTANCE);
		ts.registerType(NullType.INSTANCE);

		Set<Type> expected = new HashSet<>();
		expected.add(VoidType.INSTANCE);
		expected.add(NullType.INSTANCE);
		assertEquals(expected, Untyped.INSTANCE.allInstances(ts));
	}

}
