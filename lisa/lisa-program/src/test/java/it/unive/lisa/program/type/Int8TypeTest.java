package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class Int8TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(8, Int8Type.INSTANCE.getNBits());
		assertFalse(Int8Type.INSTANCE.isUnsigned());
		assertTrue(Int8Type.INSTANCE.isIntegral());
		assertTrue(Int8Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(Int8Type.INSTANCE, Int8Type.INSTANCE);
		assertEquals(Int8Type.INSTANCE.hashCode(), Int8Type.INSTANCE.hashCode());
	}

	@Test
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(Int8Type.INSTANCE.equals(Int16Type.INSTANCE));
		assertFalse(Int8Type.INSTANCE.equals(Float32Type.INSTANCE));
		assertFalse(Int8Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(Int8Type.INSTANCE.equals(UInt8Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(Int8Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(Int8Type.INSTANCE.canBeAssignedTo(Int64Type.INSTANCE));
		assertTrue(Int8Type.INSTANCE.canBeAssignedTo(Float64Type.INSTANCE));
		assertFalse(Int8Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(Int8Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(Int32Type.INSTANCE, Int8Type.INSTANCE.commonSupertype(Int32Type.INSTANCE));
		assertEquals(Int8Type.INSTANCE, Int8Type.INSTANCE.commonSupertype(Int8Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float32Type.INSTANCE, Int32Type.INSTANCE.commonSupertype(Float32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int8Type.INSTANCE, Int8Type.INSTANCE.commonSupertype(UInt8Type.INSTANCE));
		assertEquals(Int8Type.INSTANCE, UInt8Type.INSTANCE.commonSupertype(Int8Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, Int8Type.INSTANCE.commonSupertype(BoolType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(Int8Type.INSTANCE), Int8Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("int8", Int8Type.INSTANCE.toString());
	}

}
