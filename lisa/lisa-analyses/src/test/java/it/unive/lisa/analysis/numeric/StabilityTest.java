package it.unive.lisa.analysis.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.numeric.Trend;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.numeric.IntInterval;
import org.junit.jupiter.api.Test;

public class StabilityTest {

	Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	IntInterval interval1 = new IntInterval(3, 5);

	IntInterval interval2 = new IntInterval(3, 4);

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1top = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.TOP),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1bot = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.BOTTOM),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1st = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.STABLE),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1in = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.INC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1de = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.DEC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1nd = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_DEC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1ni = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_INC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s1ns = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_STABLE),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2top = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.TOP),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2bot = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.BOTTOM),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2st = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.STABLE),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2in = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.INC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2de = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.DEC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2nd = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_DEC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2ni = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_INC),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	ValueLatticeProduct<ValueEnvironment<Trend>,
			ValueEnvironment<IntInterval>> s2ns = new ValueLatticeProduct<>(
					new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_STABLE),
					new ValueEnvironment<>(IntInterval.TOP).putState(x, interval2));

	@Test
	public void test_Equals() {

		assertEquals(s1ns, s1ns);

		ValueLatticeProduct<ValueEnvironment<Trend>,
				ValueEnvironment<IntInterval>> s1ns_bis = new ValueLatticeProduct<>(
						new ValueEnvironment<>(new Trend()).putState(x, Trend.NON_STABLE),
						new ValueEnvironment<>(IntInterval.TOP).putState(x, interval1));

		assertEquals(s1ns, s1ns_bis);

		assertNotEquals(s1ns, s2ns);
		assertNotEquals(s1ns, s1in);
		assertNotEquals(s2ns, s1in);
	}

	@Test
	public void test_LessOrEqual()
			throws SemanticException {
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
	public void test_Lub()
			throws SemanticException {
		// lub({[3,5], INC}, {[3,5], DEC}) = {[3,5], NON_STABLE}
		assertEquals(s1in, s1in.lub(s1in));
		assertEquals(s1ns.second, s1in.second.lub(s1de.second));
		assertEquals(s1ns.first.lattice, s1in.first.lattice.lub(s1de.first.lattice));

		// lub({[3,5], INC}, {[3,5], NON_STABLE}) = {[3,5], NON_STABLE}
		assertEquals(s1ns, s1in.lub(s1de));
		assertEquals(s1ns, s1de.lub(s1in));

		// lub({[3,5], NON_DEC},{[3,5], DEC})= {[3,5], TOP}
		assertEquals(s1ns, s1in.lub(s1ns));
		assertEquals(s1ns, s1ns.lub(s1in));

		assertEquals(s1top, s1nd.lub(s1de));
	}

}
