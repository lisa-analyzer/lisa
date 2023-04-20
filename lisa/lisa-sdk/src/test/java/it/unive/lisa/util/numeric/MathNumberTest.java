package it.unive.lisa.util.numeric;

import static it.unive.lisa.util.numeric.MathNumber.MINUS_INFINITY;
import static it.unive.lisa.util.numeric.MathNumber.MINUS_ONE;
import static it.unive.lisa.util.numeric.MathNumber.NaN;
import static it.unive.lisa.util.numeric.MathNumber.ONE;
import static it.unive.lisa.util.numeric.MathNumber.PLUS_INFINITY;
import static it.unive.lisa.util.numeric.MathNumber.ZERO;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MathNumberTest {

	@Test
	public void testAdditionCornerCases() {
		assertEquals("+n + +inf != +inf", PLUS_INFINITY, ONE.add(PLUS_INFINITY));
		assertEquals("+inf + +n != +inf", PLUS_INFINITY, PLUS_INFINITY.add(ONE));
		assertEquals("-n + +inf != +inf", PLUS_INFINITY, MINUS_ONE.add(PLUS_INFINITY));
		assertEquals("+inf + -n != +inf", PLUS_INFINITY, PLUS_INFINITY.add(MINUS_ONE));
		assertEquals("0 + +inf != +inf", PLUS_INFINITY, ZERO.add(PLUS_INFINITY));
		assertEquals("+inf + 0 != +inf", PLUS_INFINITY, PLUS_INFINITY.add(ZERO));

		assertEquals("+n + -inf != +inf", MINUS_INFINITY, ONE.add(MINUS_INFINITY));
		assertEquals("-inf + +n != +inf", MINUS_INFINITY, MINUS_INFINITY.add(ONE));
		assertEquals("-n + -inf != +inf", MINUS_INFINITY, MINUS_ONE.add(MINUS_INFINITY));
		assertEquals("-inf + -n != +inf", MINUS_INFINITY, MINUS_INFINITY.add(MINUS_ONE));
		assertEquals("0 + -inf != +inf", MINUS_INFINITY, ZERO.add(MINUS_INFINITY));
		assertEquals("-inf + 0 != +inf", MINUS_INFINITY, MINUS_INFINITY.add(ZERO));

		assertEquals("+inf + +inf != +inf", PLUS_INFINITY, PLUS_INFINITY.add(PLUS_INFINITY));
		assertEquals("+inf + -inf != NaN", NaN, PLUS_INFINITY.add(MINUS_INFINITY));
		assertEquals("-inf + +inf != NaN", NaN, MINUS_INFINITY.add(PLUS_INFINITY));
		assertEquals("-inf + -inf != -inf", MINUS_INFINITY, MINUS_INFINITY.add(MINUS_INFINITY));
	}

	@Test
	public void testSubtractionCornerCases() {
		assertEquals("+n - +inf != -inf", MINUS_INFINITY, ONE.subtract(PLUS_INFINITY));
		assertEquals("+inf - +n != +inf", PLUS_INFINITY, PLUS_INFINITY.subtract(ONE));
		assertEquals("-n - +inf != -inf", MINUS_INFINITY, MINUS_ONE.subtract(PLUS_INFINITY));
		assertEquals("+inf - -n != +inf", PLUS_INFINITY, PLUS_INFINITY.subtract(MINUS_ONE));
		assertEquals("0 - +inf != -inf", MINUS_INFINITY, ZERO.subtract(PLUS_INFINITY));
		assertEquals("+inf - 0 != +inf", PLUS_INFINITY, PLUS_INFINITY.subtract(ZERO));

		assertEquals("+n - -inf != +inf", PLUS_INFINITY, ONE.subtract(MINUS_INFINITY));
		assertEquals("-inf - +n != +inf", MINUS_INFINITY, MINUS_INFINITY.subtract(ONE));
		assertEquals("-n - -inf != +inf", PLUS_INFINITY, MINUS_ONE.subtract(MINUS_INFINITY));
		assertEquals("-inf - -n != +inf", MINUS_INFINITY, MINUS_INFINITY.subtract(MINUS_ONE));
		assertEquals("0 - -inf != +inf", PLUS_INFINITY, ZERO.subtract(MINUS_INFINITY));
		assertEquals("-inf - 0 != +inf", MINUS_INFINITY, MINUS_INFINITY.subtract(ZERO));

		assertEquals("+inf - +inf != NaN", NaN, PLUS_INFINITY.subtract(PLUS_INFINITY));
		assertEquals("+inf - -inf != +inf", PLUS_INFINITY, PLUS_INFINITY.subtract(MINUS_INFINITY));
		assertEquals("-inf - +inf != -inf", MINUS_INFINITY, MINUS_INFINITY.subtract(PLUS_INFINITY));
		assertEquals("-inf - -inf != NaN", NaN, MINUS_INFINITY.subtract(MINUS_INFINITY));
	}

	@Test
	public void testMultiplicationCornerCases() {
		assertEquals("+n * +inf != +inf", PLUS_INFINITY, ONE.multiply(PLUS_INFINITY));
		assertEquals("+inf * +n != +inf", PLUS_INFINITY, PLUS_INFINITY.multiply(ONE));
		assertEquals("-n * +inf != +inf", MINUS_INFINITY, MINUS_ONE.multiply(PLUS_INFINITY));
		assertEquals("+inf * -n != +inf", MINUS_INFINITY, PLUS_INFINITY.multiply(MINUS_ONE));
		assertEquals("0 * +inf != NaN", NaN, ZERO.multiply(PLUS_INFINITY));
		assertEquals("+inf * 0 != NaN", NaN, PLUS_INFINITY.multiply(ZERO));

		assertEquals("+n * -inf != +inf", MINUS_INFINITY, ONE.multiply(MINUS_INFINITY));
		assertEquals("-inf * +n != +inf", MINUS_INFINITY, MINUS_INFINITY.multiply(ONE));
		assertEquals("-n * -inf != +inf", PLUS_INFINITY, MINUS_ONE.multiply(MINUS_INFINITY));
		assertEquals("-inf * -n != +inf", PLUS_INFINITY, MINUS_INFINITY.multiply(MINUS_ONE));
		assertEquals("0 * -inf != NaN", NaN, ZERO.multiply(MINUS_INFINITY));
		assertEquals("-inf * 0 != NaN", NaN, MINUS_INFINITY.multiply(ZERO));

		assertEquals("+inf * +inf != +inf", PLUS_INFINITY, PLUS_INFINITY.multiply(PLUS_INFINITY));
		assertEquals("+inf * -inf != +inf", MINUS_INFINITY, PLUS_INFINITY.multiply(MINUS_INFINITY));
		assertEquals("-inf * +inf != +inf", MINUS_INFINITY, MINUS_INFINITY.multiply(PLUS_INFINITY));
		assertEquals("-inf * -inf != -inf", PLUS_INFINITY, MINUS_INFINITY.multiply(MINUS_INFINITY));
	}

	@Test
	public void testDivisionCornerCases() {
		assertEquals("+n / +inf != +inf", ZERO, ONE.divide(PLUS_INFINITY));
		assertEquals("+inf / +n != +inf", PLUS_INFINITY, PLUS_INFINITY.divide(ONE));
		assertEquals("-n / +inf != +inf", ZERO, MINUS_ONE.divide(PLUS_INFINITY));
		assertEquals("+inf / -n != +inf", MINUS_INFINITY, PLUS_INFINITY.divide(MINUS_ONE));
		assertEquals("0 / +inf != +inf", ZERO, ZERO.divide(PLUS_INFINITY));
		assertEquals("+inf / 0 != nan", NaN, PLUS_INFINITY.divide(ZERO));

		assertEquals("+n / -inf != +inf", ZERO, ONE.divide(MINUS_INFINITY));
		assertEquals("-inf / +n != +inf", MINUS_INFINITY, MINUS_INFINITY.divide(ONE));
		assertEquals("-n / -inf != +inf", ZERO, MINUS_ONE.divide(MINUS_INFINITY));
		assertEquals("-inf / -n != +inf", PLUS_INFINITY, MINUS_INFINITY.divide(MINUS_ONE));
		assertEquals("0 / -inf != +inf", ZERO, ZERO.divide(MINUS_INFINITY));
		assertEquals("-inf / 0 != NaN", NaN, MINUS_INFINITY.divide(ZERO));

		assertEquals("+inf / +inf != +inf", NaN, PLUS_INFINITY.divide(PLUS_INFINITY));
		assertEquals("+inf / -inf != +inf", NaN, PLUS_INFINITY.divide(MINUS_INFINITY));
		assertEquals("-inf / +inf != +inf", NaN, MINUS_INFINITY.divide(PLUS_INFINITY));
		assertEquals("-inf / -inf != -inf", NaN, MINUS_INFINITY.divide(MINUS_INFINITY));
	}

	@Test
	public void testNaN() {
		assertEquals("+n + nan != nan", NaN, ONE.add(NaN));
		assertEquals("nan + +n != nan", NaN, NaN.add(ONE));
		assertEquals("-n + nan != nan", NaN, MINUS_ONE.add(NaN));
		assertEquals("nan + -n != nan", NaN, NaN.add(MINUS_ONE));
		assertEquals("0 + nan != nan", NaN, ZERO.add(NaN));
		assertEquals("nan + 0 != nan", NaN, NaN.add(ZERO));
		assertEquals("+inf + nan != nan", NaN, PLUS_INFINITY.add(NaN));
		assertEquals("nan + +inf != nan", NaN, NaN.add(PLUS_INFINITY));
		assertEquals("-inf + nan != nan", NaN, MINUS_INFINITY.add(NaN));
		assertEquals("nan + -inf != nan", NaN, NaN.add(MINUS_INFINITY));

		assertEquals("+n - nan != nan", NaN, ONE.subtract(NaN));
		assertEquals("nan - +n != nan", NaN, NaN.subtract(ONE));
		assertEquals("-n - nan != nan", NaN, MINUS_ONE.subtract(NaN));
		assertEquals("nan - -n != nan", NaN, NaN.subtract(MINUS_ONE));
		assertEquals("0 - nan != nan", NaN, ZERO.subtract(NaN));
		assertEquals("nan - 0 != nan", NaN, NaN.subtract(ZERO));
		assertEquals("+inf - nan != nan", NaN, PLUS_INFINITY.subtract(NaN));
		assertEquals("nan - +inf != nan", NaN, NaN.subtract(PLUS_INFINITY));
		assertEquals("-inf - nan != nan", NaN, MINUS_INFINITY.subtract(NaN));
		assertEquals("nan - -inf != nan", NaN, NaN.subtract(MINUS_INFINITY));

		assertEquals("+n * nan != nan", NaN, ONE.multiply(NaN));
		assertEquals("nan * +n != nan", NaN, NaN.multiply(ONE));
		assertEquals("-n * nan != nan", NaN, MINUS_ONE.multiply(NaN));
		assertEquals("nan * -n != nan", NaN, NaN.multiply(MINUS_ONE));
		assertEquals("0 * nan != nan", NaN, ZERO.multiply(NaN));
		assertEquals("nan * 0 != nan", NaN, NaN.multiply(ZERO));
		assertEquals("+inf * nan != nan", NaN, PLUS_INFINITY.multiply(NaN));
		assertEquals("nan * +inf != nan", NaN, NaN.multiply(PLUS_INFINITY));
		assertEquals("-inf * nan != nan", NaN, MINUS_INFINITY.multiply(NaN));
		assertEquals("nan * -inf != nan", NaN, NaN.multiply(MINUS_INFINITY));

		assertEquals("+n / nan != nan", NaN, ONE.divide(NaN));
		assertEquals("nan / +n != nan", NaN, NaN.divide(ONE));
		assertEquals("-n / nan != nan", NaN, MINUS_ONE.divide(NaN));
		assertEquals("nan / -n != nan", NaN, NaN.divide(MINUS_ONE));
		assertEquals("0 / nan != nan", NaN, ZERO.divide(NaN));
		assertEquals("nan / 0 != nan", NaN, NaN.divide(ZERO));
		assertEquals("+inf / nan != nan", NaN, PLUS_INFINITY.divide(NaN));
		assertEquals("nan / +inf != nan", NaN, NaN.divide(PLUS_INFINITY));
		assertEquals("-inf / nan != nan", NaN, MINUS_INFINITY.divide(NaN));
		assertEquals("nan / -inf != nan", NaN, NaN.divide(MINUS_INFINITY));

		assertEquals("+n min nan != nan", NaN, ONE.min(NaN));
		assertEquals("nan min +n != nan", NaN, NaN.min(ONE));
		assertEquals("-n min nan != nan", NaN, MINUS_ONE.min(NaN));
		assertEquals("nan min -n != nan", NaN, NaN.min(MINUS_ONE));
		assertEquals("0 min nan != nan", NaN, ZERO.min(NaN));
		assertEquals("nan min 0 != nan", NaN, NaN.min(ZERO));
		assertEquals("nan min +inf != nan", NaN, NaN.min(PLUS_INFINITY));
		assertEquals("+inf min nan != nan", NaN, PLUS_INFINITY.min(NaN));
		assertEquals("nan min -inf != nan", NaN, NaN.min(MINUS_INFINITY));
		assertEquals("-inf min nan != nan", NaN, MINUS_INFINITY.min(NaN));

		assertEquals("+n max nan != nan", NaN, ONE.max(NaN));
		assertEquals("nan max +n != nan", NaN, NaN.max(ONE));
		assertEquals("-n max nan != nan", NaN, MINUS_ONE.max(NaN));
		assertEquals("nan max -n != nan", NaN, NaN.max(MINUS_ONE));
		assertEquals("0 max nan != nan", NaN, ZERO.max(NaN));
		assertEquals("nan max 0 != nan", NaN, NaN.max(ZERO));
		assertEquals("nan max +inf != nan", NaN, NaN.max(PLUS_INFINITY));
		assertEquals("+inf max nan != nan", NaN, PLUS_INFINITY.max(NaN));
		assertEquals("nan max -inf != nan", NaN, NaN.max(MINUS_INFINITY));
		assertEquals("-inf max nan != nan", NaN, MINUS_INFINITY.max(NaN));
	}

	@Test
	public void testMinCornerCases() {
		assertEquals("+n min +inf != +n", ONE, ONE.min(PLUS_INFINITY));
		assertEquals("+inf min +n != +n", ONE, PLUS_INFINITY.min(ONE));
		assertEquals("-n min +inf != -n", MINUS_ONE, MINUS_ONE.min(PLUS_INFINITY));
		assertEquals("+inf min -n != -n", MINUS_ONE, PLUS_INFINITY.min(MINUS_ONE));
		assertEquals("0 min +inf != 0", ZERO, ZERO.min(PLUS_INFINITY));
		assertEquals("+inf min 0 != 0", ZERO, PLUS_INFINITY.min(ZERO));

		assertEquals("+n min -inf != -inf", MINUS_INFINITY, ONE.min(MINUS_INFINITY));
		assertEquals("-inf min +n != -inf", MINUS_INFINITY, MINUS_INFINITY.min(ONE));
		assertEquals("-n min -inf != -inf", MINUS_INFINITY, MINUS_ONE.min(MINUS_INFINITY));
		assertEquals("-inf min -n != -inf", MINUS_INFINITY, MINUS_INFINITY.min(MINUS_ONE));
		assertEquals("0 min -inf != -inf", MINUS_INFINITY, ZERO.min(MINUS_INFINITY));
		assertEquals("-inf min 0 != -inf", MINUS_INFINITY, MINUS_INFINITY.min(ZERO));

		assertEquals("+inf min +inf != -inf", PLUS_INFINITY, PLUS_INFINITY.min(PLUS_INFINITY));
		assertEquals("+inf min -inf != -inf", MINUS_INFINITY, PLUS_INFINITY.min(MINUS_INFINITY));
		assertEquals("-inf min +inf != -inf", MINUS_INFINITY, MINUS_INFINITY.min(PLUS_INFINITY));
		assertEquals("-inf min -inf != -inf", MINUS_INFINITY, MINUS_INFINITY.min(MINUS_INFINITY));
	}

	@Test
	public void testMaxCornerCases() {
		assertEquals("+n max +inf != +inf", PLUS_INFINITY, ONE.max(PLUS_INFINITY));
		assertEquals("+inf max +n != +inf", PLUS_INFINITY, PLUS_INFINITY.max(ONE));
		assertEquals("-n max +inf != +inf", PLUS_INFINITY, MINUS_ONE.max(PLUS_INFINITY));
		assertEquals("+inf max -n != +inf", PLUS_INFINITY, PLUS_INFINITY.max(MINUS_ONE));
		assertEquals("0 max +inf != +inf", PLUS_INFINITY, ZERO.max(PLUS_INFINITY));
		assertEquals("+inf max 0 != +inf", PLUS_INFINITY, PLUS_INFINITY.max(ZERO));

		assertEquals("+n max -inf != +n", ONE, ONE.max(MINUS_INFINITY));
		assertEquals("-inf max +n != +n", ONE, MINUS_INFINITY.max(ONE));
		assertEquals("-n max -inf != -n", MINUS_ONE, MINUS_ONE.max(MINUS_INFINITY));
		assertEquals("-inf max -n != -n", MINUS_ONE, MINUS_INFINITY.max(MINUS_ONE));
		assertEquals("0 max -inf != 0", ZERO, ZERO.max(MINUS_INFINITY));
		assertEquals("-inf max 0 != 0", ZERO, MINUS_INFINITY.max(ZERO));

		assertEquals("+inf max +inf != -inf", PLUS_INFINITY, PLUS_INFINITY.max(PLUS_INFINITY));
		assertEquals("+inf max -inf != -inf", PLUS_INFINITY, PLUS_INFINITY.max(MINUS_INFINITY));
		assertEquals("-inf max +inf != -inf", PLUS_INFINITY, MINUS_INFINITY.max(PLUS_INFINITY));
		assertEquals("-inf max -inf != -inf", MINUS_INFINITY, MINUS_INFINITY.max(MINUS_INFINITY));
	}
}
