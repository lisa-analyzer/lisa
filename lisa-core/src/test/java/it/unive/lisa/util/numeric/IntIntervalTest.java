package it.unive.lisa.util.numeric;

import static org.junit.Assert.assertEquals;

import java.util.function.BiFunction;
import org.junit.Test;

public class IntIntervalTest {

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

	private static void test(Integer x1, Integer x2, Integer y1, Integer y2, Integer e1, Integer e2, String symbol,
			BiFunction<IntInterval, IntInterval, IntInterval> operator) {
		IntInterval x = new IntInterval(x1, x2);
		IntInterval y = new IntInterval(y1, y2);
		IntInterval actual = operator.apply(x, y);
		IntInterval expected = new IntInterval(e1, e2);
		assertEquals(x + " " + symbol + " " + y + " = " + expected + " (got " + actual + ")", expected, actual);
	}

	private static void mul(Integer x1, Integer x2, Integer y1, Integer y2, Integer e1, Integer e2) {
		test(x1, x2, y1, y2, e1, e2, "*", IntInterval::mul);
	}

	@Test
	public void testMultiplication() {
		mul(-2, -1, -2, -1, 1, 4);
		mul(-2, -1, -2, 3, -6, 4);
		mul(-2, -1, 1, 2, -4, -1);
		mul(-2, 1, -2, -1, -2, 4);
		mul(-2, 1, -2, 2, -4, 4);
		mul(-2, 2, -1, 2, -4, 4);
		mul(-2, 2, -2, 1, -4, 4);
		mul(-1, 2, -2, 2, -4, 4);
		mul(-2, 1, 1, 2, -4, 2);
		mul(1, 2, -2, -1, -4, -1);
		mul(1, 2, -2, 1, -4, 2);
		mul(1, 2, 1, 2, 1, 4);
		mul(null, 0, null, 0, 0, null);
		mul(null, 0, null, null, null, null);
		mul(null, 0, 0, null, null, 0);
		mul(null, 0, 0, 0, 0, 0);
		mul(null, null, null, 0, null, null);
		mul(null, null, null, null, null, null);
		mul(null, null, 0, null, null, null);
		mul(null, null, 0, 0, 0, 0);
		mul(0, null, null, 0, null, 0);
		mul(0, null, null, null, null, null);
		mul(0, null, 0, null, 0, null);
		mul(0, null, 0, 0, 0, 0);
		mul(0, 0, null, 0, 0, 0);
		mul(0, 0, null, null, 0, 0);
		mul(0, 0, 0, null, 0, 0);
		mul(0, 0, 0, 0, 0, 0);
	}

	private static void div(Integer x1, Integer x2, Integer y1, Integer y2, Integer e1, Integer e2,
			boolean ignoreZero) {
		test(x1, x2, y1, y2, e1, e2, "/", (l, r) -> l.div(r, ignoreZero, false));
	}

	@Test
	public void testDivision() {
		div(-2, -1, -2, -1, 0, 2, false);
		div(-2, -1, -2, 3, null, null, false);
		div(-2, -1, 1, 2, -2, 0, false);
		div(-2, 1, -2, -1, -1, 2, false);
		div(-2, 1, -2, 2, null, null, false);
		div(-2, 2, -1, 2, null, null, false);
		div(-2, 2, -2, 1, null, null, false);
		div(-1, 2, -2, 2, null, null, false);
		div(-2, 1, 1, 2, -2, 1, false);
		div(1, 2, -2, -1, -2, 0, false);
		div(1, 2, -2, 1, null, null, false);
		div(1, 2, 1, 2, 0, 2, false);
		div(null, 0, null, 0, 0, null, false);
		div(null, 0, null, null, null, null, false);
		div(null, 0, 0, null, null, 0, false);
		div(null, null, null, 0, null, null, false);
		div(null, null, null, null, null, null, false);
		div(null, null, 0, null, null, null, false);
		div(0, null, null, 0, null, 0, false);
		div(0, null, null, null, null, null, false);
		div(0, null, 0, null, 0, null, false);
		div(0, 0, null, 0, 0, 0, false);
		div(0, 0, null, null, 0, 0, false);
		div(0, 0, 0, null, 0, 0, false);
	}

	@Test
	public void testDivisionIgnoreZero() {
		div(-2, -1, -2, -1, 0, 2, true);
		div(-2, -1, -2, 3, -1, 1, true);
		div(-2, -1, 1, 2, -2, 0, true);
		div(-2, 1, -2, -1, -1, 2, true);
		div(-2, 1, -2, 2, -1, 1, true);
		div(-2, 2, -1, 2, -2, 2, true);
		div(-2, 2, -2, 1, -2, 2, true);
		div(-1, 2, -2, 2, -1, 1, true);
		div(-2, 1, 1, 2, -2, 1, true);
		div(1, 2, -2, -1, -2, 0, true);
		div(1, 2, -2, 1, -1, 2, true);
		div(1, 2, 1, 2, 0, 2, true);
		div(null, 0, null, 0, 0, null, true);
		div(null, 0, null, null, 0, 0, true);
		div(null, 0, 0, null, null, 0, true);
		div(null, null, null, 0, null, null, true);
		div(null, null, null, null, 0, 0, true);
		div(null, null, 0, null, null, null, true);
		div(0, null, null, 0, null, 0, true);
		div(0, null, null, null, 0, 0, true);
		div(0, null, 0, null, 0, null, true);
		div(0, 0, null, 0, 0, 0, true);
		div(0, 0, null, null, 0, 0, true);
		div(0, 0, 0, null, 0, 0, true);
	}
}
