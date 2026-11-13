package it.unive.lisa.symbolic.value.operator.binary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NumericOperationTest {

	static class FakeNumericType
			implements
			NumericType {

		final int nbits;
		final boolean unsigned;
		final boolean integral;

		FakeNumericType(
				int nbits,
				boolean unsigned,
				boolean integral) {
			this.nbits = nbits;
			this.unsigned = unsigned;
			this.integral = integral;
		}

		@Override
		public int getNBits() {
			return nbits;
		}

		@Override
		public boolean isUnsigned() {
			return unsigned;
		}

		@Override
		public boolean isIntegral() {
			return integral;
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

		@Override
		public boolean equals(
				Object obj) {
			if (!(obj instanceof FakeNumericType))
				return false;
			FakeNumericType o = (FakeNumericType) obj;
			return nbits == o.nbits && unsigned == o.unsigned && integral == o.integral;
		}

		@Override
		public int hashCode() {
			return Objects.hash(nbits, unsigned, integral);
		}
	}

	static class FakeNonNumericType
			implements
			Type {

		// AccessModifiers#testVisibilityOfTypes requires every concrete
		// Type to expose a public/protected no-arg constructor
		public FakeNonNumericType() {
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

	static class FakeTypeSystem
			extends
			TypeSystem {

		// a fixed singleton, like a real TypeSystem would return: callers
		// (including typeInference() itself) must be able to rely on
		// getBooleanType() always yielding the same, equal instance
		private final BooleanType booleanType = new BooleanType() {

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
		};

		@Override
		public BooleanType getBooleanType() {
			return booleanType;
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
			return false;
		}

		@Override
		public int distanceBetweenTypes(
				Type first,
				Type second) {
			return 0;
		}
	}

	private static final FakeTypeSystem TS = new FakeTypeSystem();
	private static final FakeNumericType INT32 = new FakeNumericType(32, false, true);
	private static final FakeNumericType UINT32 = new FakeNumericType(32, true, true);

	// a concrete, representative leaf class: it must not override
	// typeInference, so exercising it exercises NumericOperation itself
	private static final NumericOperation OP = it.unive.lisa.symbolic.value.operator.binary.Numeric32BitAdd.INSTANCE;

	private static Set<Type> set(
			Type... types) {
		return new HashSet<>(java.util.Arrays.asList(types));
	}

	@Test
	public void delegatesDirectlyToCommonNumericalType() {
		assertEquals(NumericType.commonNumericalType(set(INT32), set(INT32)),
				OP.typeInference(TS, set(INT32), set(INT32)));
		assertEquals(
				NumericType.commonNumericalType(set(INT32), set(UINT32)),
				OP.typeInference(TS, set(INT32), set(UINT32)));
	}

	@Test
	public void bothSidesNumericAndEqualYieldsThatType() {
		assertEquals(Collections.singleton(INT32), OP.typeInference(TS, set(INT32), set(INT32)));
	}

	@Test
	public void bothSidesNumericButIncompatibleYieldsUntyped() {
		assertEquals(Collections.singleton(Untyped.INSTANCE), OP.typeInference(TS, set(INT32), set(UINT32)));
	}

	@Test
	public void oneSidePurelyUntypedIsPairedWithTheConcreteNumericOtherSide() {
		// regression test: typeInference used to short-circuit to an empty
		// set whenever a side had no *concrete* numeric type in it, even
		// though the other side was numeric and Untyped might still resolve
		// to a compatible numeric type at runtime; NumericType#
		// commonNumericalType is explicitly designed to pair Untyped with
		// the concrete other side rather than discard it
		assertEquals(Collections.singleton(INT32), OP.typeInference(TS, set(Untyped.INSTANCE), set(INT32)));
		assertEquals(Collections.singleton(INT32), OP.typeInference(TS, set(INT32), set(Untyped.INSTANCE)));
	}

	@Test
	public void bothSidesPurelyUntypedYieldsEmpty() {
		assertTrue(OP.typeInference(TS, set(Untyped.INSTANCE), set(Untyped.INSTANCE)).isEmpty());
	}

	@Test
	public void neitherSideHasAnyNumericOrUntypedTypeYieldsEmpty() {
		assertTrue(OP.typeInference(TS, set(new FakeNonNumericType()), set(new FakeNonNumericType())).isEmpty());
	}

	@Test
	public void nonNumericTypesAreIgnoredWhenAGenuineNumericTypeIsAlsoPresent() {
		Set<Type> result = OP.typeInference(TS, set(INT32, new FakeNonNumericType()), set(INT32));
		assertEquals(Collections.singleton(INT32), result);
	}

	@Test
	public void mixedUntypedAndNumericPairsWithUntypedOnlyOtherSide() {
		Set<Type> result = OP.typeInference(TS, set(Untyped.INSTANCE, INT32), set(Untyped.INSTANCE));
		assertEquals(set(Untyped.INSTANCE, INT32), result);
	}

}
