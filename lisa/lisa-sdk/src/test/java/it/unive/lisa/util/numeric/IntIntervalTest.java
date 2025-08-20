package it.unive.lisa.util.numeric;

import static org.junit.Assert.assertEquals;

import java.util.function.BiFunction;
import org.junit.Test;

public class IntIntervalTest {

	private static void test(
			IntInterval x,
			IntInterval y,
			IntInterval expected,
			String symbol,
			BiFunction<IntInterval, IntInterval, IntInterval> operator) {
		IntInterval actual = operator.apply(x, y);
		assertEquals(x + " " + symbol + " " + y + " = " + expected + " (got " + actual + ")", expected, actual);
	}

	private static void test(
			int x1,
			int x2,
			int y1,
			int y2,
			IntInterval expected,
			String symbol,
			BiFunction<IntInterval, IntInterval, IntInterval> operator) {
		IntInterval x = new IntInterval(x1, x2);
		IntInterval y = new IntInterval(y1, y2);
		test(x, y, expected, symbol, operator);
	}

	private static void test(
			int x1,
			int x2,
			int y1,
			int y2,
			int e1,
			int e2,
			String symbol,
			BiFunction<IntInterval, IntInterval, IntInterval> operator) {
		IntInterval x = new IntInterval(x1, x2);
		IntInterval y = new IntInterval(y1, y2);
		IntInterval expected = new IntInterval(e1, e2);
		test(x, y, expected, symbol, operator);
	}

	private static final IntInterval MI_ZERO = new IntInterval(null, 0);

	private static final IntInterval ZERO_PI = new IntInterval(0, null);

	@Test
	public void testBaseArithmetic() {
		IntInterval x = new IntInterval(1, 1);
		IntInterval y = new IntInterval(10, 10);
		IntInterval z = x.div(y, false, true);

		assertEquals(new IntInterval(0, 1), z);

		x = new IntInterval(1, 2);
		y = new IntInterval(3, 4);

		assertEquals(new IntInterval(4, 6), x.plus(y));
		assertEquals(new IntInterval(-3, -1), x.diff(y));
		assertEquals(new IntInterval(3, 8), x.mul(y));
		assertEquals(new IntInterval(0, 1), x.div(y, false, true));
	}

	@Test
	public void testMultiplication() {
		test(-2, -1, -2, -1, 1, 4, "*", IntInterval::mul);
		test(-2, -1, -2, 3, -6, 4, "*", IntInterval::mul);
		test(-2, -1, 1, 2, -4, -1, "*", IntInterval::mul);
		test(-2, 1, -2, -1, -2, 4, "*", IntInterval::mul);
		test(-2, 1, -2, 2, -4, 4, "*", IntInterval::mul);
		test(-2, 2, -1, 2, -4, 4, "*", IntInterval::mul);
		test(-2, 2, -2, 1, -4, 4, "*", IntInterval::mul);
		test(-1, 2, -2, 2, -4, 4, "*", IntInterval::mul);
		test(-2, 1, 1, 2, -4, 2, "*", IntInterval::mul);
		test(1, 2, -2, -1, -4, -1, "*", IntInterval::mul);
		test(1, 2, -2, 1, -4, 2, "*", IntInterval::mul);
		test(1, 2, 1, 2, 1, 4, "*", IntInterval::mul);
		test(MI_ZERO, MI_ZERO, IntInterval.NaN, "*", IntInterval::mul);
		test(MI_ZERO, IntInterval.TOP, IntInterval.TOP, "*", IntInterval::mul);
		test(MI_ZERO, ZERO_PI, IntInterval.NaN, "*", IntInterval::mul);
		test(MI_ZERO, IntInterval.ZERO, IntInterval.ZERO, "*", IntInterval::mul);
		test(IntInterval.TOP, MI_ZERO, IntInterval.TOP, "*", IntInterval::mul);
		test(IntInterval.TOP, IntInterval.TOP, IntInterval.TOP, "*", IntInterval::mul);
		test(IntInterval.TOP, ZERO_PI, IntInterval.TOP, "*", IntInterval::mul);
		test(IntInterval.TOP, IntInterval.ZERO, IntInterval.ZERO, "*", IntInterval::mul);
		test(ZERO_PI, MI_ZERO, IntInterval.NaN, "*", IntInterval::mul);
		test(ZERO_PI, IntInterval.TOP, IntInterval.TOP, "*", IntInterval::mul);
		test(ZERO_PI, ZERO_PI, ZERO_PI, "*", IntInterval::mul);
		test(ZERO_PI, IntInterval.ZERO, IntInterval.ZERO, "*", IntInterval::mul);
		test(IntInterval.ZERO, MI_ZERO, IntInterval.ZERO, "*", IntInterval::mul);
		test(IntInterval.ZERO, IntInterval.TOP, IntInterval.ZERO, "*", IntInterval::mul);
		test(IntInterval.ZERO, ZERO_PI, IntInterval.ZERO, "*", IntInterval::mul);
		test(IntInterval.ZERO, IntInterval.ZERO, IntInterval.ZERO, "*", IntInterval::mul);
	}

	@Test
	public void testDivision() {
		BiFunction<IntInterval, IntInterval, IntInterval> div = (
				l,
				r) -> l.div(r, false, false);
		test(-2, -1, -2, -1, 0, 2, "/", div);
		test(-2, -1, -2, 3, IntInterval.TOP, "/", div);
		test(-2, -1, 1, 2, -2, 0, "/", div);
		test(-2, 1, -2, -1, -1, 2, "/", div);
		test(-2, 1, -2, 2, IntInterval.TOP, "/", div);
		test(-2, 2, -1, 2, IntInterval.TOP, "/", div);
		test(-2, 2, -2, 1, IntInterval.TOP, "/", div);
		test(-1, 2, -2, 2, IntInterval.TOP, "/", div);
		test(-2, 1, 1, 2, -2, 1, "/", div);
		test(1, 2, -2, -1, -2, 0, "/", div);
		test(1, 2, -2, 1, IntInterval.TOP, "/", div);
		test(1, 2, 1, 2, 0, 2, "/", div);
		test(MI_ZERO, MI_ZERO, IntInterval.NaN, "/", div);
		test(MI_ZERO, IntInterval.TOP, IntInterval.NaN, "/", div);
		test(MI_ZERO, ZERO_PI, IntInterval.NaN, "/", div);
		test(IntInterval.TOP, MI_ZERO, IntInterval.TOP, "/", div);
		test(IntInterval.TOP, IntInterval.TOP, IntInterval.TOP, "/", div);
		test(IntInterval.TOP, ZERO_PI, IntInterval.TOP, "/", div);
		test(ZERO_PI, MI_ZERO, IntInterval.NaN, "/", div);
		test(ZERO_PI, IntInterval.TOP, IntInterval.NaN, "/", div);
		test(ZERO_PI, ZERO_PI, ZERO_PI, "/", div);
		test(IntInterval.ZERO, MI_ZERO, IntInterval.ZERO, "/", div);
		test(IntInterval.ZERO, IntInterval.TOP, IntInterval.ZERO, "/", div);
		test(IntInterval.ZERO, ZERO_PI, IntInterval.ZERO, "/", div);
	}

	@Test
	public void testDivisionIgnoreZero() {
		BiFunction<IntInterval, IntInterval, IntInterval> div = (
				l,
				r) -> l.div(r, true, false);
		test(-2, -1, -2, -1, 0, 2, "/", div);
		test(-2, -1, -2, 3, -1, 1, "/", div);
		test(-2, -1, 1, 2, -2, 0, "/", div);
		test(-2, 1, -2, -1, -1, 2, "/", div);
		test(-2, 1, -2, 2, -1, 1, "/", div);
		test(-2, 2, -1, 2, -2, 2, "/", div);
		test(-2, 2, -2, 1, -2, 2, "/", div);
		test(-1, 2, -2, 2, -1, 1, "/", div);
		test(-2, 1, 1, 2, -2, 1, "/", div);
		test(1, 2, -2, -1, -2, 0, "/", div);
		test(1, 2, -2, 1, -1, 2, "/", div);
		test(1, 2, 1, 2, 0, 2, "/", div);
		test(MI_ZERO, MI_ZERO, IntInterval.NaN, "/", div);
		test(MI_ZERO, IntInterval.TOP, IntInterval.ZERO, "/", div);
		test(MI_ZERO, ZERO_PI, IntInterval.NaN, "/", div);
		test(IntInterval.TOP, MI_ZERO, IntInterval.TOP, "/", div);
		test(IntInterval.TOP, IntInterval.TOP, IntInterval.ZERO, "/", div);
		test(IntInterval.TOP, ZERO_PI, IntInterval.TOP, "/", div);
		test(ZERO_PI, MI_ZERO, IntInterval.NaN, "/", div);
		test(ZERO_PI, IntInterval.TOP, IntInterval.ZERO, "/", div);
		test(ZERO_PI, ZERO_PI, ZERO_PI, "/", div);
		test(IntInterval.ZERO, MI_ZERO, IntInterval.ZERO, "/", div);
		test(IntInterval.ZERO, IntInterval.TOP, IntInterval.ZERO, "/", div);
		test(IntInterval.ZERO, ZERO_PI, IntInterval.ZERO, "/", div);
	}

}
