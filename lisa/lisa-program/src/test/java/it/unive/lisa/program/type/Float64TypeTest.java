package it.unive.lisa.program.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

public class Float64TypeTest {

	@Test
	public void reportsItsOwnProperties() {
		assertEquals(64, Float64Type.INSTANCE.getNBits());
		assertFalse(Float64Type.INSTANCE.isUnsigned());
		assertFalse(Float64Type.INSTANCE.isIntegral());
		assertTrue(Float64Type.INSTANCE.isSigned());
	}

	@Test
	public void equalsHoldsForItself() {
		assertEquals(Float64Type.INSTANCE, Float64Type.INSTANCE);
		assertEquals(Float64Type.INSTANCE.hashCode(), Float64Type.INSTANCE.hashCode());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void equalsFailsForDifferentBitWidthOrIntegralness() {
		assertFalse(Float64Type.INSTANCE.equals(Float32Type.INSTANCE));
		assertFalse(Float64Type.INSTANCE.equals(Int64Type.INSTANCE));
		assertFalse(Float64Type.INSTANCE.equals(UInt64Type.INSTANCE));
		assertFalse(Float64Type.INSTANCE.equals(BoolType.INSTANCE));
	}

	@Test
	public void canBeAssignedToNumericTypesAndUntypedOnly() {
		assertTrue(Float64Type.INSTANCE.canBeAssignedTo(it.unive.lisa.type.Untyped.INSTANCE));
		assertTrue(Float64Type.INSTANCE.canBeAssignedTo(Int8Type.INSTANCE));
		assertTrue(Float64Type.INSTANCE.canBeAssignedTo(Float32Type.INSTANCE));
		assertFalse(Float64Type.INSTANCE.canBeAssignedTo(BoolType.INSTANCE));
		assertFalse(Float64Type.INSTANCE.canBeAssignedTo(StringType.INSTANCE));
	}

	@Test
	public void commonSupertypeWidensToTheLargerBitWidth() {
		assertEquals(Float64Type.INSTANCE, Float32Type.INSTANCE.commonSupertype(Float64Type.INSTANCE));
		assertEquals(Float64Type.INSTANCE, Float64Type.INSTANCE.commonSupertype(Float32Type.INSTANCE));
	}

	@Test
	public void commonSupertypePrefersNonIntegralAtEqualBitWidth() {
		assertEquals(Float64Type.INSTANCE, Float64Type.INSTANCE.commonSupertype(Int64Type.INSTANCE));
		assertEquals(Float64Type.INSTANCE, Float64Type.INSTANCE.commonSupertype(UInt64Type.INSTANCE));
	}

	@Test
	public void commonSupertypeWithNonNumericIsUntyped() {
		assertEquals(it.unive.lisa.type.Untyped.INSTANCE, Float64Type.INSTANCE.commonSupertype(BoolType.INSTANCE));
	}

	@Test
	public void allInstancesIsJustItself() {
		assertEquals(Collections.singleton(Float64Type.INSTANCE), Float64Type.INSTANCE.allInstances(null));
	}

	@Test
	public void toStringIdentifiesTheType() {
		assertEquals("float64", Float64Type.INSTANCE.toString());
	}

}
