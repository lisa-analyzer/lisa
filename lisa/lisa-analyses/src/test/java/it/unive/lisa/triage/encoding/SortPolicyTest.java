package it.unive.lisa.triage.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Float32Type;
import it.unive.lisa.program.type.Float64Type;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.triage.smt.Sort;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the sorts decided by {@link SortPolicy}, in particular for the
 * encoding of floating point numbers.
 */
public class SortPolicyTest {

	private final SortPolicy policy = new SortPolicy(Map.of());

	/**
	 * Floating point numbers are encoded with the sort of their width.
	 */
	@Test
	public void floatsHaveTheSortOfTheirWidth() {
		assertEquals(Sort.FLOAT32, policy.sortOf(Float32Type.INSTANCE));
		assertEquals(Sort.FLOAT64, policy.sortOf(Float64Type.INSTANCE));
		assertEquals(Sort.INT, policy.sortOf(Int32Type.INSTANCE));
		assertEquals(Sort.BOOL, policy.sortOf(BoolType.INSTANCE));
		assertEquals(Sort.STRING, policy.sortOf(StringType.INSTANCE));
		assertFalse(policy.encodesDoubleFloats());
	}

	/**
	 * With double floats, all floating point numbers are encoded as 64-bits
	 * ones, and the other types keep their sorts.
	 */
	@Test
	public void doubleFloatsAreAll64Bits() {
		SortPolicy doubles = policy.withDoubleFloats();
		assertTrue(doubles.encodesDoubleFloats());
		assertEquals(Sort.FLOAT64, doubles.sortOf(Float32Type.INSTANCE));
		assertEquals(Sort.FLOAT64, doubles.sortOf(Float64Type.INSTANCE));
		assertEquals(Sort.INT, doubles.sortOf(Int32Type.INSTANCE));
		// a 32-bits and a 64-bits number now share a sort
		assertEquals(Sort.FLOAT64, doubles.sortOf(Set.of(Float32Type.INSTANCE, Float64Type.INSTANCE)));
		assertEquals(ValueSort.SORT, policy.sortOf(Set.of(Float32Type.INSTANCE, Float64Type.INSTANCE)));
	}

	/**
	 * Without floats, floating point numbers are not encoded as such, also with
	 * double floats, and dropping the floats keeps the double floats.
	 */
	@Test
	public void doubleFloatsWithoutFloats() {
		SortPolicy none = policy.withDoubleFloats().withoutFloats();
		assertTrue(none.encodesDoubleFloats());
		assertFalse(none.sortOf(Float32Type.INSTANCE).equals(Sort.FLOAT32));
		assertFalse(none.sortOf(Float32Type.INSTANCE).equals(Sort.FLOAT64));
		assertFalse(policy.withoutFloats().withDoubleFloats().sortOf(Float64Type.INSTANCE).equals(Sort.FLOAT64));
	}
}
