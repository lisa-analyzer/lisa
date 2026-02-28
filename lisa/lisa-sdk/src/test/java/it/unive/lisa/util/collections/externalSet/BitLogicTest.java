package it.unive.lisa.util.collections.externalSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class BitLogicTest {

	private static final int LIMIT = 99999;

	@Test
	public void testBitmask() {
		for (int i = 0; i < 63; i++) {
			long bitmask = 1L << (i % 64);
			long pow = (long) Math.pow(2, i);
			assertEquals(pow, bitmask, "Wrong bitmask for " + i + ": expected " + pow + ", got " + bitmask);
		}
		long _63 = -9223372036854775808L; // this is 2^63 in two's complement
		long bitmask = 1L << (63 % 64);
		assertEquals(_63, bitmask, "Wrong bitmask for 63: expected " + _63 + ", got " + bitmask);
	}

	@Test
	public void testBitvectorIndex() {
		Random random = new Random();
		for (int i = 0; i < LIMIT; i++) {
			int test = Math.abs(random.nextInt());
			int index = test >> 6;
			int div = test / 64;
			assertEquals(div, index, "Wrong bitvector index for " + test + ": expected " + div + ", got " + index);
		}
	}

	@Test
	public void testSetUnset() {
		final int length = 5;
		long[] bits = new long[length];
		Random random = new Random();
		for (int i = 0; i < LIMIT; i++) {
			// length * 64 is the maximum number of positions that can be
			// represented by the bitvector
			int test = random.nextInt(length * 64);

			assertEquals(0L, smash(bits), "Testing " + test + ": the bitvector is not set to 0 before the computation");
			assertFalse(
					(bits[test >> 6] & 1L << (test % 64)) != 0L,
					"Testing " + test + ": the corresponding bit is set at the beginning");

			bits[test >> 6] |= 1L << (test % 64);
			assertNotEquals(
					0L,
					smash(bits),
					"Testing " + test + ": set() did not modify the bitvector (still equal to 0)");
			assertArrayEquals(
					new int[] { test },
					actives(bits),
					"Testing " + test + ": set() did not modfiy the bits appropriately");
			assertTrue(
					(bits[test >> 6] & 1L << (test % 64)) != 0L,
					"Testing " + test + ": isset() does not detect the modification made by set()");

			bits[test >> 6] &= ~(1L << (test % 64));
			assertEquals(0L, smash(bits), "Testing " + test + ": unset() did not bring the bitvector to 0");
			assertFalse(
					(bits[test >> 6] & 1L << (test % 64)) != 0L,
					"Testing " + test + ": isset() does not detect the modification made by unset()");
		}
	}

	private static long smash(
			long[] bits) {
		long res = 0L;
		for (long l : bits)
			res += l;
		return res;
	}

	private static int[] actives(
			long[] bits) {
		List<Integer> res = new ArrayList<>();
		for (int i = 0; i < bits.length; i++)
			if (bits[i] != 0) {
				long l = bits[i];
				for (int pos = 0; pos < 64; pos++) {
					if ((l & 1L) == 1L)
						res.add(64 * i + pos);
					l = l >> 1;
				}
			}

		return res.stream().mapToInt(i -> i).toArray();
	}

}
