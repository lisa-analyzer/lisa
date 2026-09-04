package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class Int16TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(16, Int16Type.INSTANCE.getNBits());
		assertFalse(Int16Type.INSTANCE.isUnsigned());
		assertTrue(Int16Type.INSTANCE.isIntegral());
		assertTrue(Int16Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(Int16Type.INSTANCE, Int16Type.INSTANCE);
		assertEquals(Int16Type.INSTANCE.hashCode(), Int16Type.INSTANCE.hashCode());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(Int16Type.INSTANCE.equals(Int32Type.INSTANCE));
		assertFalse(Int16Type.INSTANCE.equals(Float32Type.INSTANCE));
		assertFalse(Int16Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsAcrossSignedness() {
		// NumericType.sameNumericTypes() (which equals() delegates to) also
		// compares isUnsigned(), so same-width same-integralness types with
		// different signedness must not be equal
		assertFalse(Int16Type.INSTANCE.equals(UInt16Type.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(Int16Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(Int16Type.INSTANCE.canBeAssignedTo(Int64Type.INSTANCE));
		assertTrue(Int16Type.INSTANCE.canBeAssignedTo(Float64Type.INSTANCE));
		assertFalse(Int16Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(Int16Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(Int64Type.INSTANCE, Int16Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
		assertEquals(Int16Type.INSTANCE, Int16Type.INSTANCE.commonSupertype(Int8Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float64Type.INSTANCE, Int64Type.INSTANCE.commonSupertype(Float64Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersSignedAtEqualBitWidthAndIntegralness() {
		assertEquals(Int16Type.INSTANCE, Int16Type.INSTANCE.commonSupertype(UInt16Type.INSTANCE));
		assertEquals(Int16Type.INSTANCE, UInt16Type.INSTANCE.commonSupertype(Int16Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, Int16Type.INSTANCE.commonSupertype(StringType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(Int16Type.INSTANCE), Int16Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("int16", Int16Type.INSTANCE.toString());
	}

}
