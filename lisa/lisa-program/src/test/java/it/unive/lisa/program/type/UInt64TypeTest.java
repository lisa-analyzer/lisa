package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class UInt64TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(64, UInt64Type.INSTANCE.getNBits());
		assertTrue(UInt64Type.INSTANCE.isUnsigned());
		assertTrue(UInt64Type.INSTANCE.isIntegral());
		assertFalse(UInt64Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(UInt64Type.INSTANCE, UInt64Type.INSTANCE);
		assertEquals(UInt64Type.INSTANCE.hashCode(), UInt64Type.INSTANCE.hashCode());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(UInt64Type.INSTANCE.equals(UInt32Type.INSTANCE));
		assertFalse(UInt64Type.INSTANCE.equals(Float64Type.INSTANCE));
		assertFalse(UInt64Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(UInt64Type.INSTANCE.equals(Int64Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(UInt64Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(UInt64Type.INSTANCE.canBeAssignedTo(Int8Type.INSTANCE));
		assertTrue(UInt64Type.INSTANCE.canBeAssignedTo(Float32Type.INSTANCE));
		assertFalse(UInt64Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(UInt64Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(UInt64Type.INSTANCE, UInt32Type.INSTANCE.commonSupertype(UInt64Type.INSTANCE));
		assertEquals(UInt64Type.INSTANCE, UInt64Type.INSTANCE.commonSupertype(UInt32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float64Type.INSTANCE, UInt64Type.INSTANCE.commonSupertype(Float64Type.INSTANCE));
		assertEquals(Float64Type.INSTANCE, Float64Type.INSTANCE.commonSupertype(UInt64Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int64Type.INSTANCE, UInt64Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
		assertEquals(Int64Type.INSTANCE, Int64Type.INSTANCE.commonSupertype(UInt64Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, UInt64Type.INSTANCE.commonSupertype(StringType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(UInt64Type.INSTANCE), UInt64Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheTypeAsUnsigned() {
		assertEquals("uint64", UInt64Type.INSTANCE.toString());
	}

}
