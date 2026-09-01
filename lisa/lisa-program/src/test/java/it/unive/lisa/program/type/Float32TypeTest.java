package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class Float32TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(32, Float32Type.INSTANCE.getNBits());
		assertFalse(Float32Type.INSTANCE.isUnsigned());
		assertFalse(Float32Type.INSTANCE.isIntegral());
		assertTrue(Float32Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(Float32Type.INSTANCE, Float32Type.INSTANCE);
		assertEquals(Float32Type.INSTANCE.hashCode(), Float32Type.INSTANCE.hashCode());
	}

	@Test
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(Float32Type.INSTANCE.equals(Float64Type.INSTANCE));
		assertFalse(Float32Type.INSTANCE.equals(Int32Type.INSTANCE));
		assertFalse(Float32Type.INSTANCE.equals(UInt32Type.INSTANCE));
		assertFalse(Float32Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(Float32Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(Float32Type.INSTANCE.canBeAssignedTo(Int64Type.INSTANCE));
		assertTrue(Float32Type.INSTANCE.canBeAssignedTo(Float64Type.INSTANCE));
		assertFalse(Float32Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(Float32Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(Float64Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(Float64Type.INSTANCE));
		assertEquals(Float32Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(Float32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float32Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(Int32Type.INSTANCE));
		assertEquals(Float32Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(UInt32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersLargerBitWidthEvenAcrossIntegralness() {
		// bit width is compared before integral-ness or sign, so a wider
		// integer still wins over this narrower float
		assertEquals(Int64Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, Float32Type.INSTANCE.commonSupertype(StringType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(Float32Type.INSTANCE), Float32Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("float32", Float32Type.INSTANCE.toString());
	}

}
