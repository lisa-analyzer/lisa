package it.unive.lisa.util.collections.externalSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class BitLogicTest {

	private static final int LIMIT = 99999;

	@Test
	public void testBitmask() {
		for (int i = 0; i < 63; i++) {
			long bitmask = 1L << (i % 64);
			long pow = (long) Math.pow(2, i);
			assertEquals("Wrong bitmask for " + i + ": expected " + pow + ", got " + bitmask, pow, bitmask);
		}
		long _63 = -9223372036854775808L; // this is 2^63 in two's complement
		long bitmask = 1L << (63 % 64);
		assertEquals("Wrong bitmask for 63: expected " + _63 + ", got " + bitmask, _63, bitmask);
	}

	@Test
	public void testBitvectorIndex() {
		Random random = new Random();
		for (int i = 0; i < LIMIT; i++) {
			int test = Math.abs(random.nextInt());
			int index = test >> 6;
			int div = test / 64;
			assertEquals("Wrong bitvector index for " + test + ": expected " + div + ", got " + index, div, index);
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

			assertEquals("Testing " + test + ": the bitvector is not set to 0 before the computation", 0L, smash(bits));
			assertFalse("Testing " + test + ": the corresponding bit is set at the beginning",
					(bits[test >> 6] & 1L << (test % 64)) != 0L);

			bits[test >> 6] |= 1L << (test % 64);
			assertNotEquals("Testing " + test + ": set() did not modify the bitvector (still equal to 0)", 0L,
					smash(bits));
			assertArrayEquals("Testing " + test + ": set() did not modfiy the bits appropriately",
					new int[] { test }, actives(bits));
			assertTrue("Testing " + test + ": isset() does not detect the modification made by set()",
					(bits[test >> 6] & 1L << (test % 64)) != 0L);

			bits[test >> 6] &= ~(1L << (test % 64));
			assertEquals("Testing " + test + ": unset() did not bring the bitvector to 0", 0L, smash(bits));
			assertFalse("Testing " + test + ": isset() does not detect the modification made by unset()",
					(bits[test >> 6] & 1L << (test % 64)) != 0L);
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
