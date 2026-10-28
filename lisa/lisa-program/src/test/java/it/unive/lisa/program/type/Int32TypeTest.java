package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class Int32TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(32, Int32Type.INSTANCE.getNBits());
		assertFalse(Int32Type.INSTANCE.isUnsigned());
		assertTrue(Int32Type.INSTANCE.isIntegral());
		assertTrue(Int32Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(Int32Type.INSTANCE, Int32Type.INSTANCE);
		assertEquals(Int32Type.INSTANCE.hashCode(), Int32Type.INSTANCE.hashCode());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(Int32Type.INSTANCE.equals(Int64Type.INSTANCE));
		assertFalse(Int32Type.INSTANCE.equals(Float32Type.INSTANCE));
		assertFalse(Int32Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(Int32Type.INSTANCE.equals(UInt32Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(Int32Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(Int32Type.INSTANCE.canBeAssignedTo(Int8Type.INSTANCE));
		assertTrue(Int32Type.INSTANCE.canBeAssignedTo(Float64Type.INSTANCE));
		assertFalse(Int32Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(Int32Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(Int64Type.INSTANCE, Int32Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
		assertEquals(Int32Type.INSTANCE, Int32Type.INSTANCE.commonSupertype(Int16Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float32Type.INSTANCE, Int32Type.INSTANCE.commonSupertype(Float32Type.INSTANCE));
		assertEquals(Float32Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(Int32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int32Type.INSTANCE, Int32Type.INSTANCE.commonSupertype(UInt32Type.INSTANCE));
		assertEquals(Int32Type.INSTANCE, UInt32Type.INSTANCE.commonSupertype(Int32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersLargerBitWidthEvenAcrossIntegralness() {
		// bit width is compared before integral-ness or sign, so a wider
		// integer still wins over a narrower float
		assertEquals(Int64Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, Int32Type.INSTANCE.commonSupertype(BoolType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(Int32Type.INSTANCE), Int32Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("int32", Int32Type.INSTANCE.toString());
	}

}
