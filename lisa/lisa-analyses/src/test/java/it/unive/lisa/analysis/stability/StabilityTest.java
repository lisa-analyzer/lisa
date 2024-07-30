package it.unive.lisa.analysis.stability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.Test;

public class StabilityTest {

	Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	Interval interval1 = new Interval(3, 5);
	Interval interval2 = new Interval(3, 4);

	Stability<ValueEnvironment<Interval>> s1top = new Stability<ValueEnvironment<Interval>>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.TOP));

	Stability<ValueEnvironment<Interval>> s1bot = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.BOTTOM));

	Stability<ValueEnvironment<Interval>> s1st = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.STABLE));

	Stability<ValueEnvironment<Interval>> s1in = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.INC));

	Stability<ValueEnvironment<Interval>> s1de = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.DEC));

	Stability<ValueEnvironment<Interval>> s1nd = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_DEC));

	Stability<ValueEnvironment<Interval>> s1ni = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_INC));

	Stability<ValueEnvironment<Interval>> s1ns = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval1),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_STABLE));

	Stability<ValueEnvironment<Interval>> s2top = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.TOP));

	Stability<ValueEnvironment<Interval>> s2bot = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.BOTTOM));

	Stability<ValueEnvironment<Interval>> s2st = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.STABLE));

	Stability<ValueEnvironment<Interval>> s2in = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.INC));

	Stability<ValueEnvironment<Interval>> s2de = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.DEC));

	Stability<ValueEnvironment<Interval>> s2nd = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_DEC));

	Stability<ValueEnvironment<Interval>> s2ni = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_INC));

	Stability<ValueEnvironment<Interval>> s2ns = new Stability<>(
			new ValueEnvironment<>(new Interval()).putState(x, interval2),
			new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_STABLE));

	@Test
	public void test_Equals() {

		assertEquals(s1ns, s1ns);

		Stability<ValueEnvironment<Interval>> s1ns_bis = new Stability<>(
				new ValueEnvironment<>(new Interval()).putState(x, interval1),
				new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_STABLE));

		assertEquals(s1ns, s1ns_bis);

		assertNotEquals(s1ns, s2ns);
		assertNotEquals(s1ns, s1in);
		assertNotEquals(s2ns, s1in);
	}

	@Test
	public void test_LessOrEqual() throws SemanticException {
		assertTrue(Trend.NON_STABLE.lessOrEqual(Trend.TOP));
		assertTrue(Trend.BOTTOM.lessOrEqual(Trend.NON_STABLE));
		assertFalse(Trend.TOP.lessOrEqual(Trend.NON_STABLE));
		assertFalse(Trend.NON_STABLE.lessOrEqual(Trend.BOTTOM));

		assertTrue(s1ns.lessOrEqual(s1ns));

		// interval <= interval && trend <= trend

		// {[3,4], INC} Le {[3,5], NON_STABLE}
		assertTrue(s2in.lessOrEqual(s1ns));
		// {[3,4], INC} <= {[3,5], INC}
		assertTrue(s2in.lessOrEqual(s1in));
		// {[3,4], INC} <= {[3,4], NON_STABLE}
		assertTrue(s2in.lessOrEqual(s2ns));

		// interval <= interval && trend !<= trend

		// {[3,4], NON_STABLE} !<= {[3,5], INC}
		assertFalse(s2ns.lessOrEqual(s1in));

		// interval !<= interval && trend <= trend

		// {[3,5], INC} !<= {[3,4], INC}
		assertFalse(s1in.lessOrEqual(s2in));

		// interval !<= interval && trend !<=trend

		// {[3,5], NON_STABLE} !<= {[3,4], INC}
		assertFalse(s1ns.lessOrEqual(s2in));

		// interval <= interval && TOP !<= trend
		assertFalse(s2top.lessOrEqual(s1ns));
		assertTrue(s2ns.lessOrEqual(s2top));

		// interval !<= interval && BOTTOM <= trend
		assertFalse(s1bot.lessOrEqual(s2in));
	}

	@Test
	public void test_is() {
		assertFalse(s1ns.isBottom());
		assertFalse(s2ni.isTop());
		assertFalse(s2top.isTop());
		assertTrue(s2in.top().isTop());
		assertTrue(s2in.bottom().isBottom());
	}

	@Test
	public void test_Lub() throws SemanticException {
		// lub({[3,5], INC}, {[3,5], DEC}) = {[3,5], NON_STABLE}
		assertEquals(s1in, s1in.lub(s1in));
		assertEquals(s1ns.getAuxiliaryDomain(), s1in.getAuxiliaryDomain().lub(s1de.getAuxiliaryDomain()));
		assertEquals(s1ns.getTrends().lattice, s1in.getTrends().lattice.lub(s1de.getTrends().lattice));

		// lub({[3,5], INC}, {[3,5], NON_STABLE}) = {[3,5], NON_STABLE}
		assertEquals(s1ns, s1in.lub(s1de));
		assertEquals(s1ns, s1de.lub(s1in));

		// lub({[3,5], NON_DEC},{[3,5], DEC})= {[3,5], TOP}
		assertEquals(s1ns, s1in.lub(s1ns));
		assertEquals(s1ns, s1ns.lub(s1in));

		assertEquals(s1top, s1nd.lub(s1de));
	}

}
