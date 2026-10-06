package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NumericTypeTest {

	private static class FakeNumericType
			implements
			NumericType {

		private final int nbits;
		private final boolean unsigned;
		private final boolean integral;

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

		@Override
		public String toString() {
			return "num" + nbits + (unsigned ? "u" : "s") + (integral ? "i" : "f");
		}
	}

	private static class FakeNonNumericType
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

	private static final FakeNumericType INT8 = new FakeNumericType(8, false, true);
	private static final FakeNumericType INT32 = new FakeNumericType(32, false, true);
	private static final FakeNumericType UINT32 = new FakeNumericType(32, true, true);
	private static final FakeNumericType FLOAT32 = new FakeNumericType(32, false, false);

	@Test
	public void bitWidthPredicatesReflectGetNBits() {
		assertTrue(INT8.is8Bits());
		assertFalse(INT8.is16Bits());
		assertFalse(INT8.is32Bits());
		assertFalse(INT8.is64Bits());
		assertTrue(INT32.is32Bits());
		assertFalse(INT32.is8Bits());
	}

	@Test
	public void isSignedIsTheOppositeOfIsUnsigned() {
		assertTrue(INT32.isSigned());
		assertFalse(UINT32.isSigned());
	}

	@Test
	public void sameNumericTypesRequiresAllThreeAttributesToMatch() {
		assertTrue(INT32.sameNumericTypes(new FakeNumericType(32, false, true)));
		assertFalse(INT32.sameNumericTypes(UINT32));
		assertFalse(INT32.sameNumericTypes(FLOAT32));
		assertFalse(INT32.sameNumericTypes(INT8));
	}

	@Test
	public void supertypeGivesPrecedenceToLargerBitWidth() {
		assertSame(INT32, INT8.supertype(INT32));
		assertSame(INT32, INT32.supertype(INT8));
	}

	@Test
	public void supertypeGivesPrecedenceToNonIntegralOverIntegralAtEqualWidth() {
		assertSame(FLOAT32, INT32.supertype(FLOAT32));
		assertSame(FLOAT32, FLOAT32.supertype(INT32));
	}

	@Test
	public void supertypeGivesPrecedenceToSignedOverUnsignedAtEqualWidthAndIntegrality() {
		assertSame(INT32, INT32.supertype(UINT32));
		assertSame(INT32, UINT32.supertype(INT32));
	}

	@Test
	public void supertypeReturnsThisWhenFullyTied() {
		FakeNumericType other = new FakeNumericType(32, false, true);
		assertSame(INT32, INT32.supertype(other));
	}

	@Test
	public void commonNumericalTypeIsEmptyWhenNeitherSideHasANumericType() {
		Set<Type> left = new HashSet<>(java.util.Arrays.asList(new FakeNonNumericType(), Untyped.INSTANCE));
		Set<Type> right = new HashSet<>(Collections.singletonList(Untyped.INSTANCE));
		assertTrue(NumericType.commonNumericalType(left, right).isEmpty());
	}

	@Test
	public void commonNumericalTypeIgnoresNonNumericNonUntypedInputs() {
		Set<Type> left = new HashSet<>(java.util.Arrays.asList(INT32, new FakeNonNumericType()));
		Set<Type> right = new HashSet<>(Collections.singletonList(INT32));
		Set<Type> result = NumericType.commonNumericalType(left, right);
		assertEquals(Collections.singleton(INT32), result);
	}

	@Test
	public void commonNumericalTypePairsUntypedWithTheConcreteOtherSide() {
		Set<Type> left = new HashSet<>(Collections.singletonList(Untyped.INSTANCE));
		Set<Type> right = new HashSet<>(Collections.singletonList(INT32));
		assertEquals(Collections.singleton(INT32), NumericType.commonNumericalType(left, right));
		assertEquals(Collections.singleton(INT32), NumericType.commonNumericalType(right, left));
	}

	@Test
	public void commonNumericalTypeOfTwoUntypedIsEmptyWhenNoSideHasANumericType() {
		// a side made exclusively of Untyped does not count as "having a
		// numeric type", so the whole computation short-circuits to empty
		Set<Type> both = new HashSet<>(Collections.singletonList(Untyped.INSTANCE));
		assertTrue(NumericType.commonNumericalType(both, both).isEmpty());
	}

	@Test
	public void commonNumericalTypePairsUntypedWithUntypedWhenAtLeastOneSideHasANumericType() {
		// left has a numeric type alongside Untyped, so the computation does
		// not short-circuit, and the Untyped/Untyped pair contributes Untyped
		// to the result, alongside the Untyped/INT32 pair contributing INT32
		Set<Type> left = new HashSet<>(java.util.Arrays.asList(Untyped.INSTANCE, INT32));
		Set<Type> right = new HashSet<>(Collections.singletonList(Untyped.INSTANCE));
		Set<Type> expected = new HashSet<>(java.util.Arrays.asList(Untyped.INSTANCE, INT32));
		assertEquals(expected, NumericType.commonNumericalType(left, right));
	}

	@Test
	public void commonNumericalTypeOfDistinctNumericTypesUsesCommonSupertype() {
		// FakeNumericType.commonSupertype falls back to Untyped for anything
		// that is not exactly equal to itself
		Set<Type> left = new HashSet<>(Collections.singletonList(INT32));
		Set<Type> right = new HashSet<>(Collections.singletonList(UINT32));
		assertEquals(Collections.singleton(Untyped.INSTANCE), NumericType.commonNumericalType(left, right));
	}

	@Test
	public void castIsConversionDefaultsToTrueForNumericTypes() {
		assertTrue(INT32.castIsConversion());
	}

}
