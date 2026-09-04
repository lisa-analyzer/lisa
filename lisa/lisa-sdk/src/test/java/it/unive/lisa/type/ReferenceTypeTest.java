package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ReferenceTypeTest {

	private static class Labeled
			implements
			Type {

		private final String label;
		private final Set<Type> assignableTo;
		private final Set<Type> ownInstances;

		Labeled(
				String label,
				Type... assignableTo) {
			this.label = label;
			this.assignableTo = new HashSet<>(Arrays.asList(assignableTo));
			this.ownInstances = Collections.singleton(this);
		}

		Labeled(
				String label,
				Set<Type> ownInstances) {
			this.label = label;
			this.assignableTo = Collections.emptySet();
			this.ownInstances = ownInstances;
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return equals(other) || assignableTo.contains(other);
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return ownInstances;
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof Labeled && label.equals(((Labeled) obj).label);
		}

		@Override
		public int hashCode() {
			return label.hashCode();
		}

		@Override
		public String toString() {
			return label;
		}
	}

	@Test
	public void getInnerTypeReturnsTheWrappedType() {
		assertSame(Untyped.INSTANCE, new ReferenceType(Untyped.INSTANCE).getInnerType());
	}

	@Test
	public void toStringAppendsAStar() {
		assertEquals("untyped*", new ReferenceType(Untyped.INSTANCE).toString());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheInnerType() {
		ReferenceType a = new ReferenceType(new Labeled("a"));
		ReferenceType b = new ReferenceType(new Labeled("a"));
		ReferenceType c = new ReferenceType(new Labeled("b"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(c));
	}

	@Test
	public void canBeAssignedToIsCovariantOnTheInnerTypeOrUntyped() {
		Labeled a = new Labeled("a");
		Labeled b = new Labeled("b", a);
		ReferenceType refA = new ReferenceType(a);
		ReferenceType refB = new ReferenceType(b);

		// b can be assigned to a (configured above), so ref(b) can be
		// assigned to ref(a)
		assertTrue(refB.canBeAssignedTo(refA));
		assertFalse(refA.canBeAssignedTo(refB));
		assertTrue(refA.canBeAssignedTo(Untyped.INSTANCE));
		assertFalse(refA.canBeAssignedTo(a));
	}

	@Test
	public void commonSupertypeOfEqualReferencesIsItself() {
		ReferenceType a = new ReferenceType(new Labeled("a"));
		ReferenceType b = new ReferenceType(new Labeled("a"));
		assertSame(a, a.commonSupertype(b));
	}

	@Test
	public void commonSupertypeOfTwoDifferentReferencesWrapsTheInnerCommonSupertype() {
		Labeled a = new Labeled("a");
		ReferenceType refA = new ReferenceType(a);
		ReferenceType refDifferent = new ReferenceType(new Labeled("different"));

		Type result = refA.commonSupertype(refDifferent);
		assertTrue(result instanceof ReferenceType);
		assertEquals(Untyped.INSTANCE, ((ReferenceType) result).getInnerType());
	}

	@Test
	public void commonSupertypeWithANonReferenceTypeIsUntyped() {
		ReferenceType refA = new ReferenceType(new Labeled("a"));
		assertSame(Untyped.INSTANCE, refA.commonSupertype(VoidType.INSTANCE));
	}

	@Test
	public void allInstancesWrapsEachInnerInstancePlusItself() {
		Labeled a = new Labeled("a");
		Labeled b = new Labeled("b");
		Set<Type> innerInstances = new HashSet<>(Arrays.asList(a, b));
		Labeled multi = new Labeled("multi", innerInstances);

		ReferenceType ref = new ReferenceType(multi);
		Set<Type> result = ref.allInstances(new MinimalTypeSystem());

		Set<Type> expected = new HashSet<>(Arrays.asList(
				new ReferenceType(a),
				new ReferenceType(b),
				ref));
		assertEquals(expected, result);
	}

}
