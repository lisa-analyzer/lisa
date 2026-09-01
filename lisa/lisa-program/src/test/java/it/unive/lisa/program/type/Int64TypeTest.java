package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class Int64TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(64, Int64Type.INSTANCE.getNBits());
		assertFalse(Int64Type.INSTANCE.isUnsigned());
		assertTrue(Int64Type.INSTANCE.isIntegral());
		assertTrue(Int64Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(Int64Type.INSTANCE, Int64Type.INSTANCE);
		assertEquals(Int64Type.INSTANCE.hashCode(), Int64Type.INSTANCE.hashCode());
	}

	@Test
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(Int64Type.INSTANCE.equals(Int32Type.INSTANCE));
		assertFalse(Int64Type.INSTANCE.equals(Float64Type.INSTANCE));
		assertFalse(Int64Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(Int64Type.INSTANCE.equals(UInt64Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(Int64Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(Int64Type.INSTANCE.canBeAssignedTo(Int8Type.INSTANCE));
		assertTrue(Int64Type.INSTANCE.canBeAssignedTo(Float32Type.INSTANCE));
		assertFalse(Int64Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(Int64Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(Int64Type.INSTANCE, Int32Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
		assertEquals(Int64Type.INSTANCE, Int64Type.INSTANCE.commonSupertype(Int32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float64Type.INSTANCE, Int64Type.INSTANCE.commonSupertype(Float64Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int64Type.INSTANCE, Int64Type.INSTANCE.commonSupertype(UInt64Type.INSTANCE));
		assertEquals(Int64Type.INSTANCE, UInt64Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, Int64Type.INSTANCE.commonSupertype(StringType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(Int64Type.INSTANCE), Int64Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("int64", Int64Type.INSTANCE.toString());
	}

}
