package it.unive.lisa.util.numeric;

import static it.unive.lisa.util.numeric.MathNumber.MINUS_INFINITY;
import static it.unive.lisa.util.numeric.MathNumber.MINUS_ONE;
import static it.unive.lisa.util.numeric.MathNumber.NaN;
import static it.unive.lisa.util.numeric.MathNumber.ONE;
import static it.unive.lisa.util.numeric.MathNumber.PLUS_INFINITY;
import static it.unive.lisa.util.numeric.MathNumber.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MathNumberTest {

	@Test
	public void testAdditionCornerCases() {
		assertEquals(PLUS_INFINITY, ONE.add(PLUS_INFINITY), "+n + +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.add(ONE), "+inf + +n != +inf");
		assertEquals(PLUS_INFINITY, MINUS_ONE.add(PLUS_INFINITY), "-n + +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.add(MINUS_ONE), "+inf + -n != +inf");
		assertEquals(PLUS_INFINITY, ZERO.add(PLUS_INFINITY), "0 + +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.add(ZERO), "+inf + 0 != +inf");

		assertEquals(MINUS_INFINITY, ONE.add(MINUS_INFINITY), "+n + -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.add(ONE), "-inf + +n != +inf");
		assertEquals(MINUS_INFINITY, MINUS_ONE.add(MINUS_INFINITY), "-n + -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.add(MINUS_ONE), "-inf + -n != +inf");
		assertEquals(MINUS_INFINITY, ZERO.add(MINUS_INFINITY), "0 + -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.add(ZERO), "-inf + 0 != +inf");

		assertEquals(PLUS_INFINITY, PLUS_INFINITY.add(PLUS_INFINITY), "+inf + +inf != +inf");
		assertEquals(NaN, PLUS_INFINITY.add(MINUS_INFINITY), "+inf + -inf != NaN");
		assertEquals(NaN, MINUS_INFINITY.add(PLUS_INFINITY), "-inf + +inf != NaN");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.add(MINUS_INFINITY), "-inf + -inf != -inf");
	}

	@Test
	public void testSubtractionCornerCases() {
		assertEquals(MINUS_INFINITY, ONE.subtract(PLUS_INFINITY), "+n - +inf != -inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.subtract(ONE), "+inf - +n != +inf");
		assertEquals(MINUS_INFINITY, MINUS_ONE.subtract(PLUS_INFINITY), "-n - +inf != -inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.subtract(MINUS_ONE), "+inf - -n != +inf");
		assertEquals(MINUS_INFINITY, ZERO.subtract(PLUS_INFINITY), "0 - +inf != -inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.subtract(ZERO), "+inf - 0 != +inf");

		assertEquals(PLUS_INFINITY, ONE.subtract(MINUS_INFINITY), "+n - -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.subtract(ONE), "-inf - +n != +inf");
		assertEquals(PLUS_INFINITY, MINUS_ONE.subtract(MINUS_INFINITY), "-n - -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.subtract(MINUS_ONE), "-inf - -n != +inf");
		assertEquals(PLUS_INFINITY, ZERO.subtract(MINUS_INFINITY), "0 - -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.subtract(ZERO), "-inf - 0 != +inf");

		assertEquals(NaN, PLUS_INFINITY.subtract(PLUS_INFINITY), "+inf - +inf != NaN");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.subtract(MINUS_INFINITY), "+inf - -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.subtract(PLUS_INFINITY), "-inf - +inf != -inf");
		assertEquals(NaN, MINUS_INFINITY.subtract(MINUS_INFINITY), "-inf - -inf != NaN");
	}

	@Test
	public void testMultiplicationCornerCases() {
		assertEquals(PLUS_INFINITY, ONE.multiply(PLUS_INFINITY), "+n * +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.multiply(ONE), "+inf * +n != +inf");
		assertEquals(MINUS_INFINITY, MINUS_ONE.multiply(PLUS_INFINITY), "-n * +inf != +inf");
		assertEquals(MINUS_INFINITY, PLUS_INFINITY.multiply(MINUS_ONE), "+inf * -n != +inf");
		assertEquals(NaN, ZERO.multiply(PLUS_INFINITY), "0 * +inf != NaN");
		assertEquals(NaN, PLUS_INFINITY.multiply(ZERO), "+inf * 0 != NaN");

		assertEquals(MINUS_INFINITY, ONE.multiply(MINUS_INFINITY), "+n * -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.multiply(ONE), "-inf * +n != +inf");
		assertEquals(PLUS_INFINITY, MINUS_ONE.multiply(MINUS_INFINITY), "-n * -inf != +inf");
		assertEquals(PLUS_INFINITY, MINUS_INFINITY.multiply(MINUS_ONE), "-inf * -n != +inf");
		assertEquals(NaN, ZERO.multiply(MINUS_INFINITY), "0 * -inf != NaN");
		assertEquals(NaN, MINUS_INFINITY.multiply(ZERO), "-inf * 0 != NaN");

		assertEquals(PLUS_INFINITY, PLUS_INFINITY.multiply(PLUS_INFINITY), "+inf * +inf != +inf");
		assertEquals(MINUS_INFINITY, PLUS_INFINITY.multiply(MINUS_INFINITY), "+inf * -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.multiply(PLUS_INFINITY), "-inf * +inf != +inf");
		assertEquals(PLUS_INFINITY, MINUS_INFINITY.multiply(MINUS_INFINITY), "-inf * -inf != -inf");
	}

	@Test
	public void testDivisionCornerCases() {
		assertEquals(ZERO, ONE.divide(PLUS_INFINITY), "+n / +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.divide(ONE), "+inf / +n != +inf");
		assertEquals(ZERO, MINUS_ONE.divide(PLUS_INFINITY), "-n / +inf != +inf");
		assertEquals(MINUS_INFINITY, PLUS_INFINITY.divide(MINUS_ONE), "+inf / -n != +inf");
		assertEquals(ZERO, ZERO.divide(PLUS_INFINITY), "0 / +inf != +inf");
		assertEquals(NaN, PLUS_INFINITY.divide(ZERO), "+inf / 0 != nan");

		assertEquals(ZERO, ONE.divide(MINUS_INFINITY), "+n / -inf != +inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.divide(ONE), "-inf / +n != +inf");
		assertEquals(ZERO, MINUS_ONE.divide(MINUS_INFINITY), "-n / -inf != +inf");
		assertEquals(PLUS_INFINITY, MINUS_INFINITY.divide(MINUS_ONE), "-inf / -n != +inf");
		assertEquals(ZERO, ZERO.divide(MINUS_INFINITY), "0 / -inf != +inf");
		assertEquals(NaN, MINUS_INFINITY.divide(ZERO), "-inf / 0 != NaN");

		assertEquals(NaN, PLUS_INFINITY.divide(PLUS_INFINITY), "+inf / +inf != +inf");
		assertEquals(NaN, PLUS_INFINITY.divide(MINUS_INFINITY), "+inf / -inf != +inf");
		assertEquals(NaN, MINUS_INFINITY.divide(PLUS_INFINITY), "-inf / +inf != +inf");
		assertEquals(NaN, MINUS_INFINITY.divide(MINUS_INFINITY), "-inf / -inf != -inf");
	}

	@Test
	public void testNaN() {
		assertEquals(NaN, ONE.add(NaN), "+n + nan != nan");
		assertEquals(NaN, NaN.add(ONE), "nan + +n != nan");
		assertEquals(NaN, MINUS_ONE.add(NaN), "-n + nan != nan");
		assertEquals(NaN, NaN.add(MINUS_ONE), "nan + -n != nan");
		assertEquals(NaN, ZERO.add(NaN), "0 + nan != nan");
		assertEquals(NaN, NaN.add(ZERO), "nan + 0 != nan");
		assertEquals(NaN, PLUS_INFINITY.add(NaN), "+inf + nan != nan");
		assertEquals(NaN, NaN.add(PLUS_INFINITY), "nan + +inf != nan");
		assertEquals(NaN, MINUS_INFINITY.add(NaN), "-inf + nan != nan");
		assertEquals(NaN, NaN.add(MINUS_INFINITY), "nan + -inf != nan");

		assertEquals(NaN, ONE.subtract(NaN), "+n - nan != nan");
		assertEquals(NaN, NaN.subtract(ONE), "nan - +n != nan");
		assertEquals(NaN, MINUS_ONE.subtract(NaN), "-n - nan != nan");
		assertEquals(NaN, NaN.subtract(MINUS_ONE), "nan - -n != nan");
		assertEquals(NaN, ZERO.subtract(NaN), "0 - nan != nan");
		assertEquals(NaN, NaN.subtract(ZERO), "nan - 0 != nan");
		assertEquals(NaN, PLUS_INFINITY.subtract(NaN), "+inf - nan != nan");
		assertEquals(NaN, NaN.subtract(PLUS_INFINITY), "nan - +inf != nan");
		assertEquals(NaN, MINUS_INFINITY.subtract(NaN), "-inf - nan != nan");
		assertEquals(NaN, NaN.subtract(MINUS_INFINITY), "nan - -inf != nan");

		assertEquals(NaN, ONE.multiply(NaN), "+n * nan != nan");
		assertEquals(NaN, NaN.multiply(ONE), "nan * +n != nan");
		assertEquals(NaN, MINUS_ONE.multiply(NaN), "-n * nan != nan");
		assertEquals(NaN, NaN.multiply(MINUS_ONE), "nan * -n != nan");
		assertEquals(NaN, ZERO.multiply(NaN), "0 * nan != nan");
		assertEquals(NaN, NaN.multiply(ZERO), "nan * 0 != nan");
		assertEquals(NaN, PLUS_INFINITY.multiply(NaN), "+inf * nan != nan");
		assertEquals(NaN, NaN.multiply(PLUS_INFINITY), "nan * +inf != nan");
		assertEquals(NaN, MINUS_INFINITY.multiply(NaN), "-inf * nan != nan");
		assertEquals(NaN, NaN.multiply(MINUS_INFINITY), "nan * -inf != nan");

		assertEquals(NaN, ONE.divide(NaN), "+n / nan != nan");
		assertEquals(NaN, NaN.divide(ONE), "nan / +n != nan");
		assertEquals(NaN, MINUS_ONE.divide(NaN), "-n / nan != nan");
		assertEquals(NaN, NaN.divide(MINUS_ONE), "nan / -n != nan");
		assertEquals(NaN, ZERO.divide(NaN), "0 / nan != nan");
		assertEquals(NaN, NaN.divide(ZERO), "nan / 0 != nan");
		assertEquals(NaN, PLUS_INFINITY.divide(NaN), "+inf / nan != nan");
		assertEquals(NaN, NaN.divide(PLUS_INFINITY), "nan / +inf != nan");
		assertEquals(NaN, MINUS_INFINITY.divide(NaN), "-inf / nan != nan");
		assertEquals(NaN, NaN.divide(MINUS_INFINITY), "nan / -inf != nan");

		assertEquals(NaN, ONE.min(NaN), "+n min nan != nan");
		assertEquals(NaN, NaN.min(ONE), "nan min +n != nan");
		assertEquals(NaN, MINUS_ONE.min(NaN), "-n min nan != nan");
		assertEquals(NaN, NaN.min(MINUS_ONE), "nan min -n != nan");
		assertEquals(NaN, ZERO.min(NaN), "0 min nan != nan");
		assertEquals(NaN, NaN.min(ZERO), "nan min 0 != nan");
		assertEquals(NaN, NaN.min(PLUS_INFINITY), "nan min +inf != nan");
		assertEquals(NaN, PLUS_INFINITY.min(NaN), "+inf min nan != nan");
		assertEquals(NaN, NaN.min(MINUS_INFINITY), "nan min -inf != nan");
		assertEquals(NaN, MINUS_INFINITY.min(NaN), "-inf min nan != nan");

		assertEquals(NaN, ONE.max(NaN), "+n max nan != nan");
		assertEquals(NaN, NaN.max(ONE), "nan max +n != nan");
		assertEquals(NaN, MINUS_ONE.max(NaN), "-n max nan != nan");
		assertEquals(NaN, NaN.max(MINUS_ONE), "nan max -n != nan");
		assertEquals(NaN, ZERO.max(NaN), "0 max nan != nan");
		assertEquals(NaN, NaN.max(ZERO), "nan max 0 != nan");
		assertEquals(NaN, NaN.max(PLUS_INFINITY), "nan max +inf != nan");
		assertEquals(NaN, PLUS_INFINITY.max(NaN), "+inf max nan != nan");
		assertEquals(NaN, NaN.max(MINUS_INFINITY), "nan max -inf != nan");
		assertEquals(NaN, MINUS_INFINITY.max(NaN), "-inf max nan != nan");
	}

	@Test
	public void testMinCornerCases() {
		assertEquals(ONE, ONE.min(PLUS_INFINITY), "+n min +inf != +n");
		assertEquals(ONE, PLUS_INFINITY.min(ONE), "+inf min +n != +n");
		assertEquals(MINUS_ONE, MINUS_ONE.min(PLUS_INFINITY), "-n min +inf != -n");
		assertEquals(MINUS_ONE, PLUS_INFINITY.min(MINUS_ONE), "+inf min -n != -n");
		assertEquals(ZERO, ZERO.min(PLUS_INFINITY), "0 min +inf != 0");
		assertEquals(ZERO, PLUS_INFINITY.min(ZERO), "+inf min 0 != 0");

		assertEquals(MINUS_INFINITY, ONE.min(MINUS_INFINITY), "+n min -inf != -inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.min(ONE), "-inf min +n != -inf");
		assertEquals(MINUS_INFINITY, MINUS_ONE.min(MINUS_INFINITY), "-n min -inf != -inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.min(MINUS_ONE), "-inf min -n != -inf");
		assertEquals(MINUS_INFINITY, ZERO.min(MINUS_INFINITY), "0 min -inf != -inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.min(ZERO), "-inf min 0 != -inf");

		assertEquals(PLUS_INFINITY, PLUS_INFINITY.min(PLUS_INFINITY), "+inf min +inf != -inf");
		assertEquals(MINUS_INFINITY, PLUS_INFINITY.min(MINUS_INFINITY), "+inf min -inf != -inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.min(PLUS_INFINITY), "-inf min +inf != -inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.min(MINUS_INFINITY), "-inf min -inf != -inf");
	}

	@Test
	public void testMaxCornerCases() {
		assertEquals(PLUS_INFINITY, ONE.max(PLUS_INFINITY), "+n max +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.max(ONE), "+inf max +n != +inf");
		assertEquals(PLUS_INFINITY, MINUS_ONE.max(PLUS_INFINITY), "-n max +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.max(MINUS_ONE), "+inf max -n != +inf");
		assertEquals(PLUS_INFINITY, ZERO.max(PLUS_INFINITY), "0 max +inf != +inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.max(ZERO), "+inf max 0 != +inf");

		assertEquals(ONE, ONE.max(MINUS_INFINITY), "+n max -inf != +n");
		assertEquals(ONE, MINUS_INFINITY.max(ONE), "-inf max +n != +n");
		assertEquals(MINUS_ONE, MINUS_ONE.max(MINUS_INFINITY), "-n max -inf != -n");
		assertEquals(MINUS_ONE, MINUS_INFINITY.max(MINUS_ONE), "-inf max -n != -n");
		assertEquals(ZERO, ZERO.max(MINUS_INFINITY), "0 max -inf != 0");
		assertEquals(ZERO, MINUS_INFINITY.max(ZERO), "-inf max 0 != 0");

		assertEquals(PLUS_INFINITY, PLUS_INFINITY.max(PLUS_INFINITY), "+inf max +inf != -inf");
		assertEquals(PLUS_INFINITY, PLUS_INFINITY.max(MINUS_INFINITY), "+inf max -inf != -inf");
		assertEquals(PLUS_INFINITY, MINUS_INFINITY.max(PLUS_INFINITY), "-inf max +inf != -inf");
		assertEquals(MINUS_INFINITY, MINUS_INFINITY.max(MINUS_INFINITY), "-inf max -inf != -inf");
	}

}
