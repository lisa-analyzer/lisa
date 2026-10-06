package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class TypeSystemTest {

	// a Type whose assignability can be configured explicitly, independently
	// of any real subtyping logic, so that cast()/convert() can be exercised
	// with precise, known outcomes
	private static class Labeled
			implements
			Type {

		private final String label;
		private final Set<Type> assignableTo;

		Labeled(
				String label,
				Type... assignableTo) {
			this.label = label;
			this.assignableTo = new HashSet<>(Arrays.asList(assignableTo));
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
			return Collections.singleton(this);
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

	private static class FakeTypeSystem
			extends
			TypeSystem {

		private final Set<Type> referenceable;

		FakeTypeSystem(
				Type... referenceable) {
			this.referenceable = new HashSet<>(Arrays.asList(referenceable));
		}

		@Override
		public BooleanType getBooleanType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public StringType getStringType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public NumericType getIntegerType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public CharacterType getCharacterType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canBeReferenced(
				Type type) {
			return referenceable.contains(type);
		}

		@Override
		public int distanceBetweenTypes(
				Type first,
				Type second) {
			return 0;
		}
	}

	private static final Labeled INT32 = new Labeled("int32");
	private static final Labeled INT8 = new Labeled("int8", INT32);
	private static final Labeled STR = new Labeled("string");

	@Test
	public void registerTypeSucceedsOnlyOnceForEachName() {
		FakeTypeSystem ts = new FakeTypeSystem();
		assertTrue(ts.registerType(INT32));
		assertTrue(ts.getTypes().contains(INT32));

		// a distinct instance with the same toString() must be rejected, and
		// must not replace the one already registered
		Labeled duplicate = new Labeled("int32");
		assertFalse(ts.registerType(duplicate));
		assertSame(INT32, ts.getType("int32"));
	}

	@Test
	public void getTypeReturnsNullWhenNotRegistered() {
		FakeTypeSystem ts = new FakeTypeSystem();
		assertEquals(null, ts.getType("nope"));
	}

	@Test
	public void getTypesIsASnapshotNotALiveView() {
		FakeTypeSystem ts = new FakeTypeSystem();
		ts.registerType(INT32);
		Set<Type> snapshot = ts.getTypes();
		snapshot.add(STR);
		assertFalse(ts.getTypes().contains(STR));
	}

	@Test
	public void getReferenceWrapsReferenceableTypesAndRejectsOthers() {
		FakeTypeSystem ts = new FakeTypeSystem(INT32);
		ReferenceType ref = ts.getReference(INT32);
		assertSame(INT32, ref.getInnerType());

		assertThrows(IllegalArgumentException.class, () -> ts.getReference(STR));
	}

	@Test
	public void castKeepsOnlySourceTypesAssignableToAFlattenedToken() {
		FakeTypeSystem ts = new FakeTypeSystem();
		Set<Type> types = new HashSet<>(Arrays.asList(INT8, STR));
		Set<Type> tokens = Collections.singleton(new TypeTokenType(Collections.singleton(INT32)));

		Set<Type> result = ts.cast(types, tokens);
		assertEquals(Collections.singleton(INT8), result);
	}

	@Test
	public void castSetsMightFailWhenAnySourceTypeCannotBeAssigned() {
		FakeTypeSystem ts = new FakeTypeSystem();
		Set<Type> types = new HashSet<>(Arrays.asList(INT8, STR));
		Set<Type> tokens = Collections.singleton(new TypeTokenType(Collections.singleton(INT32)));

		AtomicBoolean mightFail = new AtomicBoolean(true);
		Set<Type> result = ts.cast(types, tokens, mightFail);
		assertEquals(Collections.singleton(INT8), result);
		assertTrue(mightFail.get());
	}

	@Test
	public void castIgnoresTokensThatAreNotTypeTokenTypes() {
		FakeTypeSystem ts = new FakeTypeSystem();
		Set<Type> types = Collections.singleton(INT32);
		// INT32 itself is not a TypeTokenType, so it contributes nothing
		Set<Type> tokens = Collections.singleton(INT32);

		assertTrue(ts.cast(types, tokens).isEmpty());
	}

	@Test
	public void convertKeepsTheTargetTokensReachableFromASourceType() {
		FakeTypeSystem ts = new FakeTypeSystem();
		Set<Type> types = Collections.singleton(INT8);
		Set<Type> tokens = Collections.singleton(new TypeTokenType(new HashSet<>(Arrays.asList(INT32, STR))));

		Set<Type> result = ts.convert(types, tokens);
		// INT8 can be assigned to INT32 (configured above) but not to STR
		assertEquals(Collections.singleton(INT32), result);
	}

}
