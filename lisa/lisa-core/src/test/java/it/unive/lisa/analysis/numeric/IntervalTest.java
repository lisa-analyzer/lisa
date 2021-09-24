package it.unive.lisa.analysis.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.types.IntType;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.Random;
import org.junit.Test;

public class IntervalTest {

	private static final int TEST_LIMIT = 5000;

	private final Random rand = new Random();
	private final Interval singleton = new Interval();
	private final ValueEnvironment<Interval> env = new ValueEnvironment<>(singleton);
	private final ProgramPoint pp = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return new SourceCodeLocation("fake", 0, 0);
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};
	private final Variable variable = new Variable(Caches.types().mkSingletonSet(IntType.INSTANCE), "x",
			pp.getLocation());
	private final Variable varAux = new Variable(Caches.types().mkSingletonSet(IntType.INSTANCE), "aux",
			pp.getLocation());

	@Test
	public void testEvalConstant() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			assertTrue("eval(" + val + ") did not return [" + val + ", " + val + "]",
					singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val, pp.getLocation()), pp).interval
							.is(val));
		}
	}

	@Test
	public void testEvalNegationOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val, pp.getLocation()), pp);
			assertTrue("eval(-" + val + ") did not return [-" + val + ", -" + val + "]",
					singleton.evalUnaryExpression(UnaryOperator.NUMERIC_NEG, aval, pp).interval.is(-val));
		}
	}

	@Test
	public void testEvalAddOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			IntInterval exp = aval1.interval.plus(aval2.interval);
			assertEquals("eval(" + val1 + " + " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_ADD, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testEvalSubOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			IntInterval exp = aval1.interval.diff(aval2.interval);
			assertEquals("eval(" + val1 + " - " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_SUB, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testEvalMulOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			IntInterval exp = aval1.interval.mul(aval2.interval);
			assertEquals("eval(" + val1 + " * " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_MUL, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testEvalDivOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			IntInterval exp = aval1.interval.div(aval2.interval, false, false);
			assertEquals("eval(" + val1 + " / " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_DIV, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testLatticeOperationsOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			Interval lub = aval1.lub(aval2);
			Interval widen = aval1.widening(aval2);
			Interval glb = aval1.glb(aval2);
			assertTrue(aval1 + " does not include " + aval1, aval1.lessOrEqual(aval1));
			assertTrue(aval2 + " does not include " + aval2, aval2.lessOrEqual(aval2));
			assertTrue(aval1 + " lub " + aval2 + " (" + lub + ") does not include " + aval1, aval1.lessOrEqual(lub));
			assertTrue(aval1 + " lub " + aval2 + " (" + lub + ") does not include " + aval2, aval2.lessOrEqual(lub));
			assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not include " + aval1,
					aval1.lessOrEqual(widen));
			assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not include " + aval2,
					aval2.lessOrEqual(widen));
			if (val2 < val1)
				assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not have its lower bound set to -Inf",
						widen.interval.lowIsMinusInfinity());
			else
				assertFalse(aval1 + " widening " + aval2 + " (" + widen + ") has its lower bound set to -Inf",
						widen.interval.lowIsMinusInfinity());
			if (val2 > val1)
				assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not have its higher bound set to +Inf",
						widen.interval.highIsPlusInfinity());
			else
				assertFalse(aval1 + " widening " + aval2 + " (" + widen + ") has its higher bound set to +Inf",
						widen.interval.highIsPlusInfinity());
			assertTrue(aval1 + " glb " + aval2 + " (" + glb + ") is not included in " + aval1, glb.lessOrEqual(aval1));
			assertTrue(aval1 + " glb " + aval2 + " (" + glb + ") is not included in " + aval2, glb.lessOrEqual(aval2));
		}
	}

	@Test
	public void testStatisfiesEQOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			Satisfiability exp = Satisfiability.fromBoolean(val1 == val2);
			assertEquals("satisfies(" + val1 + " == " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_EQ, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesNEOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			Satisfiability exp = Satisfiability.fromBoolean(val1 != val2);
			assertEquals("satisfies(" + val1 + " != " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_NE, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesGTOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			Satisfiability exp = Satisfiability.fromBoolean(val1 > val2);
			assertEquals("satisfies(" + val1 + " > " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_GT, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesGEOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			Satisfiability exp = Satisfiability.fromBoolean(val1 >= val2);
			assertEquals("satisfies(" + val1 + " >= " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_GE, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesLTOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			Satisfiability exp = Satisfiability.fromBoolean(val1 < val2);
			assertEquals("satisfies(" + val1 + " < " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_LT, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesLEOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
			Satisfiability exp = Satisfiability.fromBoolean(val1 <= val2);
			assertEquals("satisfies(" + val1 + " <= " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_LE, aval1, aval2, pp));
		}
	}

	@Test
	public void testAssumeEQOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val, pp.getLocation()), pp);
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " == " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env, BinaryOperator.COMPARISON_EQ,
							variable, new Constant(IntType.INSTANCE, val, pp.getLocation()), pp));
		}
	}

	@Test
	public void testAssumeGTOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val + 1, pp.getLocation()),
					pp);
			// val + 1, +inf
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val + 2, pp.getLocation()), pp));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " > " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env, BinaryOperator.COMPARISON_GT,
							variable, new Constant(IntType.INSTANCE, val, pp.getLocation()), pp));
		}
	}

	@Test
	public void testAssumeGEOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val, pp.getLocation()), pp);
			// val, +inf
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val + 2, pp.getLocation()), pp));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " >= " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env, BinaryOperator.COMPARISON_GE,
							variable, new Constant(IntType.INSTANCE, val, pp.getLocation()), pp));
		}
	}

	@Test
	public void testAssumeLTOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val - 1, pp.getLocation()),
					pp);
			// -inf, val - 1
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val - 2, pp.getLocation()), pp));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " < " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env, BinaryOperator.COMPARISON_LT,
							variable, new Constant(IntType.INSTANCE, val, pp.getLocation()), pp));
		}
	}

	@Test
	public void testAssumeLEOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val, pp.getLocation()), pp);
			// -inf, val
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val - 2, pp.getLocation()), pp));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " <= " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env, BinaryOperator.COMPARISON_LE,
							variable, new Constant(IntType.INSTANCE, val, pp.getLocation()), pp));
		}
	}

	private Interval mk(int val1, int val2) throws SemanticException {
		Interval aval1 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val1, pp.getLocation()), pp);
		Interval aval2 = singleton.evalNonNullConstant(new Constant(IntType.INSTANCE, val2, pp.getLocation()), pp);
		return aval1.lub(aval2);
	}

	@Test
	public void testEvalNegation() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval = mk(val1, val2);
			Interval exp = mk(-val1, -val2);
			assertEquals("eval(-" + aval + ") did not return " + exp, exp,
					singleton.evalUnaryExpression(UnaryOperator.NUMERIC_NEG, aval, pp));
		}
	}

	@Test
	public void testEvalAdd() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			IntInterval exp = aval1.interval.plus(aval2.interval);
			assertEquals("eval(" + aval1 + " + " + aval2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_ADD, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testEvalSub() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			IntInterval exp = aval1.interval.diff(aval2.interval);
			assertEquals("eval(" + aval1 + " - " + aval2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_SUB, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testEvalMul() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			IntInterval exp = aval1.interval.mul(aval2.interval);
			assertEquals("eval(" + aval1 + " * " + aval2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_MUL, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testEvalDiv() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			IntInterval exp = aval1.interval.div(aval2.interval, false, false);
			assertEquals("eval(" + aval1 + " / " + aval2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(BinaryOperator.NUMERIC_DIV, aval1, aval2, pp).interval);
		}
	}

	@Test
	public void testLatticeOperations() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			Interval lub = aval1.lub(aval2);
			Interval widen = aval1.widening(aval2);
			Interval glb = aval1.glb(aval2);
			assertTrue(aval1 + " does not include " + aval1, aval1.lessOrEqual(aval1));
			assertTrue(aval2 + " does not include " + aval2, aval2.lessOrEqual(aval2));
			assertTrue(aval1 + " lub " + aval2 + " (" + lub + ") does not include " + aval1, aval1.lessOrEqual(lub));
			assertTrue(aval1 + " lub " + aval2 + " (" + lub + ") does not include " + aval2, aval2.lessOrEqual(lub));
			assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not include " + aval1,
					aval1.lessOrEqual(widen));
			assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not include " + aval2,
					aval2.lessOrEqual(widen));
			if (aval2.interval.getLow().compareTo(aval1.interval.getLow()) < 0)
				assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not have its lower bound set to -Inf",
						widen.interval.lowIsMinusInfinity());
			else
				assertEquals(
						aval1 + " widening " + aval2 + " (" + widen
								+ ") has its lower bound different from the original",
						aval1.interval.getLow(), widen.interval.getLow());
			if (aval2.interval.getHigh().compareTo(aval1.interval.getHigh()) > 0)
				assertTrue(aval1 + " widening " + aval2 + " (" + widen + ") does not have its higher bound set to +Inf",
						widen.interval.highIsPlusInfinity());
			else
				assertEquals(
						aval1 + " widening " + aval2 + " (" + widen
								+ ") has its higher bound different from the original",
						aval1.interval.getHigh(), widen.interval.getHigh());
			assertTrue(aval1 + " glb " + aval2 + " (" + glb + ") is not included in " + aval1, glb.lessOrEqual(aval1));
			assertTrue(aval1 + " glb " + aval2 + " (" + glb + ") is not included in " + aval2, glb.lessOrEqual(aval2));
		}
	}

	@Test
	public void testStatisfiesEQ() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			Satisfiability exp = Satisfiability.fromBoolean(aval1.equals(aval2) && aval1.interval.isSingleton());
			exp = exp.lub(Satisfiability.fromBoolean(aval1.interval.intersects(aval2.interval)));
			assertEquals("satisfies(" + aval1 + " == " + aval2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_EQ, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesNE() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			Satisfiability exp = Satisfiability.fromBoolean(!aval1.interval.intersects(aval2.interval));
			if (exp == Satisfiability.NOT_SATISFIED)
				exp = Satisfiability.UNKNOWN;
			assertEquals("satisfies(" + aval1 + " != " + aval2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_NE, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesGT() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			Satisfiability exp;
			if (aval1.interval.intersects(aval2.interval))
				exp = Satisfiability.UNKNOWN;
			else
				exp = Satisfiability.fromBoolean(aval1.interval.getLow().compareTo(aval2.interval.getHigh()) > 0);
			assertEquals("satisfies(" + aval1 + " > " + aval2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_GT, aval1, aval2, pp));
		}
	}

//	@Test
//	public void custom() throws SemanticException {
//
//		int val1 = -570621533;
//		int val2 =  1276735385;
//		int val3 = -1134921951;
//		int val4 =  1341802015;
//		Interval aval1 = mk(val1, val2);
//		Interval aval2 = mk(val3, val4);
//		aval1.widening(aval2);
//	}

	@Test
	public void testStatisfiesGE() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			Satisfiability exp;
			if (aval1.interval.intersects(aval2.interval))
				exp = Satisfiability.UNKNOWN;
			else
				exp = Satisfiability.fromBoolean(aval1.interval.getLow().compareTo(aval2.interval.getHigh()) >= 0);
			assertEquals("satisfies(" + aval1 + " >= " + aval2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_GE, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesLT() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			Satisfiability exp;
			if (aval1.interval.intersects(aval2.interval))
				exp = Satisfiability.UNKNOWN;
			else
				exp = Satisfiability.fromBoolean(aval1.interval.getLow().compareTo(aval2.interval.getHigh()) < 0);
			assertEquals("satisfies(" + aval1 + " < " + aval2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_LT, aval1, aval2, pp));
		}
	}

	@Test
	public void testStatisfiesLE() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			int val3 = rand.nextInt();
			int val4 = rand.nextInt();
			Interval aval1 = mk(val1, val2);
			Interval aval2 = mk(val3, val4);
			Satisfiability exp;
			if (aval1.interval.intersects(aval2.interval))
				exp = Satisfiability.UNKNOWN;
			else
				exp = Satisfiability.fromBoolean(aval1.interval.getLow().compareTo(aval2.interval.getHigh()) <= 0);
			assertEquals("satisfies(" + aval1 + " <= " + aval2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(BinaryOperator.COMPARISON_LE, aval1, aval2, pp));
		}
	}

	@Test
	public void testAssumeEQ() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval = mk(val1, val2);
			ValueEnvironment<Interval> start = env.putState(varAux, aval);
			ValueEnvironment<Interval> exp = start.putState(variable, aval);
			assertEquals("assume(" + variable + " == " + aval + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(start, BinaryOperator.COMPARISON_EQ,
							variable, varAux, pp));
		}
	}

	@Test
	public void testAssumeGT() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval bound = mk(val1, val2);
			// low + 1, +inf
			Interval aval = val1 < val2 ? mk(val1 + 1, val2) : mk(val1, val2 + 1);
			aval = aval.widening(mk(val1 + 1, val2 + 1));
			ValueEnvironment<Interval> start = env.putState(varAux, bound);
			ValueEnvironment<Interval> exp = start.putState(variable, aval);
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start, BinaryOperator.COMPARISON_GT,
					variable, varAux, pp);
			assertEquals("assume(" + variable + " > " + bound + ") did not return " + exp, exp, act);
		}
	}

	@Test
	public void testAssumeGE() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval bound = mk(val1, val2);
			// low, +inf
			Interval aval = bound.widening(val1 < val2 ? mk(val1, val2 + 1) : mk(val1 + 1, val2));
			ValueEnvironment<Interval> start = env.putState(varAux, bound);
			ValueEnvironment<Interval> exp = start.putState(variable, aval);
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start, BinaryOperator.COMPARISON_GE,
					variable, varAux, pp);
			assertEquals("assume(" + variable + " >= " + bound + ") did not return " + exp, exp, act);
		}
	}

	@Test
	public void testAssumeLT() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval bound = mk(val1, val2);
			// -inf, high - 1
			Interval aval = val1 < val2 ? mk(val1, val2 - 1) : mk(val1 - 1, val2);
			aval = aval.widening(mk(val1 - 1, val2 - 1));
			ValueEnvironment<Interval> start = env.putState(varAux, bound);
			ValueEnvironment<Interval> exp = start.putState(variable, aval);
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start, BinaryOperator.COMPARISON_LT,
					variable, varAux, pp);
			assertEquals("assume(" + variable + " < " + bound + ") did not return " + exp, exp, act);
		}
	}

	@Test
	public void testAssumeLE() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval bound = mk(val1, val2);
			// -inf, high
			Interval aval = bound.widening(val1 < val2 ? mk(val1 - 1, val2) : mk(val1, val2 - 1));
			ValueEnvironment<Interval> start = env.putState(varAux, bound);
			ValueEnvironment<Interval> exp = start.putState(variable, aval);
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start, BinaryOperator.COMPARISON_LE,
					variable, varAux, pp);
			assertEquals("assume(" + variable + " <= " + bound + ") did not return " + exp, exp, act);
		}
	}
}
