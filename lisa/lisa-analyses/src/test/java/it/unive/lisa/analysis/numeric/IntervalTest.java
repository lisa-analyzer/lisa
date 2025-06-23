package it.unive.lisa.analysis.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.numeric.InfiniteIterationException;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class IntervalTest {

	private static final int TEST_LIMIT = 5000;

	private final Random rand = new Random();
	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);;
	private final Interval singleton = new Interval();
	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
	private final Variable varAux = new Variable(Int32Type.INSTANCE, "aux", pp.getLocation());
	private final UnaryExpression unary = new UnaryExpression(Int32Type.INSTANCE, varAux, NumericNegation.INSTANCE,
			pp.getLocation());
	private final BinaryExpression binary = new BinaryExpression(Int32Type.INSTANCE, varAux, variable,
			NumericNonOverflowingAdd.INSTANCE, pp.getLocation());
	private final ValueEnvironment<
			Interval> env = new ValueEnvironment<>(singleton).putState(variable, singleton.top());
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	@Test
	public void testEvalConstant() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			assertTrue("eval(" + val + ") did not return [" + val + ", " + val + "]",
					singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val, pp.getLocation()), pp,
							oracle).interval
									.is(val));
		}
	}

	@Test
	public void testEvalNegationOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val, pp.getLocation()), pp,
					oracle);
			assertTrue("eval(-" + val + ") did not return [-" + val + ", -" + val + "]",
					singleton.evalUnaryExpression(unary, aval, pp, oracle).interval.is(-val));
		}
	}

	@Test
	public void testEvalAddOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			IntInterval exp = aval1.interval.plus(aval2.interval);
			assertEquals("eval(" + val1 + " + " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(binary, aval1, aval2,
							pp, oracle).interval);
		}
	}

	@Test
	public void testEvalSubOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			IntInterval exp = aval1.interval.diff(aval2.interval);
			assertEquals("eval(" + val1 + " - " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(binary.withOperator(NumericNonOverflowingSub.INSTANCE), aval1, aval2,
							pp, oracle).interval);
		}
	}

	@Test
	public void testEvalMulOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			IntInterval exp = aval1.interval.mul(aval2.interval);
			assertEquals("eval(" + val1 + " * " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(binary.withOperator(NumericNonOverflowingMul.INSTANCE), aval1, aval2,
							pp, oracle).interval);
		}
	}

	@Test
	public void testEvalDivOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			IntInterval exp = aval1.interval.div(aval2.interval, false, false);
			assertEquals("eval(" + val1 + " / " + val2 + ") did not return " + exp, exp,
					singleton.evalBinaryExpression(binary.withOperator(NumericNonOverflowingDiv.INSTANCE), aval1, aval2,
							pp, oracle).interval);
		}
	}

	@Test
	public void testLatticeOperationsOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
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
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			Satisfiability exp = Satisfiability.fromBoolean(val1 == val2);
			assertEquals("satisfies(" + val1 + " == " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonEq.INSTANCE), aval1, aval2, pp,
							oracle));
		}
	}

	@Test
	public void testStatisfiesNEOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			Satisfiability exp = Satisfiability.fromBoolean(val1 != val2);
			assertEquals("satisfies(" + val1 + " != " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonNe.INSTANCE), aval1, aval2, pp,
							oracle));
		}
	}

	@Test
	public void testStatisfiesGTOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			Satisfiability exp = Satisfiability.fromBoolean(val1 > val2);
			assertEquals("satisfies(" + val1 + " > " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonGt.INSTANCE), aval1, aval2, pp,
							oracle));
		}
	}

	@Test
	public void testStatisfiesGEOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			Satisfiability exp = Satisfiability.fromBoolean(val1 >= val2);
			assertEquals("satisfies(" + val1 + " >= " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonGe.INSTANCE), aval1, aval2, pp,
							oracle));
		}
	}

	@Test
	public void testStatisfiesLTOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			Satisfiability exp = Satisfiability.fromBoolean(val1 < val2);
			assertEquals("satisfies(" + val1 + " < " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonLt.INSTANCE), aval1, aval2, pp,
							oracle));
		}
	}

	@Test
	public void testStatisfiesLEOnSingleton() {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val1 = rand.nextInt();
			int val2 = rand.nextInt();
			Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()),
					pp, oracle);
			Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()),
					pp, oracle);
			Satisfiability exp = Satisfiability.fromBoolean(val1 <= val2);
			assertEquals("satisfies(" + val1 + " <= " + val2 + ") did not return " + exp, exp,
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonLe.INSTANCE), aval1, aval2, pp,
							oracle));
		}
	}

	@Test
	public void testAssumeEQOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val, pp.getLocation()), pp,
					oracle);
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " == " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env,
							new BinaryExpression(
									BoolType.INSTANCE,
									variable,
									new Constant(Int32Type.INSTANCE, val, pp.getLocation()),
									ComparisonEq.INSTANCE,
									pp.getLocation()),
							pp,
							pp,
							oracle));
		}
	}

	@Test
	public void testAssumeGTOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val + 1, pp.getLocation()),
					pp, oracle);
			// val + 1, +inf
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val + 2, pp.getLocation()), pp,
							oracle));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " > " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env,
							new BinaryExpression(
									BoolType.INSTANCE,
									variable,
									new Constant(Int32Type.INSTANCE, val, pp.getLocation()),
									ComparisonGt.INSTANCE,
									pp.getLocation()),
							pp,
							pp,
							oracle));
		}
	}

	@Test
	public void testAssumeGEOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val, pp.getLocation()), pp,
					oracle);
			// val, +inf
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val + 2, pp.getLocation()), pp,
							oracle));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " >= " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env,
							new BinaryExpression(
									BoolType.INSTANCE,
									variable,
									new Constant(Int32Type.INSTANCE, val, pp.getLocation()),
									ComparisonGe.INSTANCE,
									pp.getLocation()),
							pp,
							pp,
							oracle));
		}
	}

	@Test
	public void testAssumeLTOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val - 1, pp.getLocation()),
					pp, oracle);
			// -inf, val - 1
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val - 2, pp.getLocation()), pp,
							oracle));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " < " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env,
							new BinaryExpression(
									BoolType.INSTANCE,
									variable,
									new Constant(Int32Type.INSTANCE, val, pp.getLocation()),
									ComparisonLt.INSTANCE,
									pp.getLocation()),
							pp,
							pp,
							oracle));
		}
	}

	@Test
	public void testAssumeLEOnSingleton() throws SemanticException {
		for (int i = 0; i < TEST_LIMIT; i++) {
			int val = rand.nextInt();
			Interval aval = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val, pp.getLocation()), pp,
					oracle);
			// -inf, val
			aval = aval.widening(
					singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val - 2, pp.getLocation()), pp,
							oracle));
			ValueEnvironment<Interval> exp = env.putState(variable, aval);
			assertEquals("assume(" + variable + " <= " + val + ") did not return " + exp, exp,
					singleton.assumeBinaryExpression(env,
							new BinaryExpression(
									BoolType.INSTANCE,
									variable,
									new Constant(Int32Type.INSTANCE, val, pp.getLocation()),
									ComparisonLe.INSTANCE,
									pp.getLocation()),
							pp,
							pp,
							oracle));
		}
	}

	private Interval mk(
			int val1,
			int val2)
			throws SemanticException {
		Interval aval1 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val1, pp.getLocation()), pp,
				oracle);
		Interval aval2 = singleton.evalNonNullConstant(new Constant(Int32Type.INSTANCE, val2, pp.getLocation()), pp,
				oracle);
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
					singleton.evalUnaryExpression(unary, aval, pp, oracle));
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
					singleton.evalBinaryExpression(binary, aval1, aval2,
							pp, oracle).interval);
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
					singleton.evalBinaryExpression(binary.withOperator(NumericNonOverflowingSub.INSTANCE), aval1, aval2,
							pp, oracle).interval);
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
					singleton.evalBinaryExpression(binary.withOperator(NumericNonOverflowingMul.INSTANCE), aval1, aval2,
							pp, oracle).interval);
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
					singleton.evalBinaryExpression(binary.withOperator(NumericNonOverflowingDiv.INSTANCE), aval1, aval2,
							pp, oracle).interval);
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
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonEq.INSTANCE), aval1, aval2, pp,
							oracle));
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
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonNe.INSTANCE), aval1, aval2, pp,
							oracle));
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
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonGt.INSTANCE), aval1, aval2, pp,
							oracle));
		}
	}

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
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonGe.INSTANCE), aval1, aval2, pp,
							oracle));
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
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonLt.INSTANCE), aval1, aval2, pp,
							oracle));
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
					singleton.satisfiesBinaryExpression(binary.withOperator(ComparisonLe.INSTANCE), aval1, aval2, pp,
							oracle));
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
					singleton.assumeBinaryExpression(start,
							new BinaryExpression(
									BoolType.INSTANCE,
									variable,
									varAux,
									ComparisonEq.INSTANCE,
									pp.getLocation()),
							pp,
							pp,
							oracle));
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
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start,
					new BinaryExpression(
							BoolType.INSTANCE,
							variable,
							varAux,
							ComparisonGt.INSTANCE,
							pp.getLocation()),
					pp,
					pp,
					oracle);
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
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start,
					new BinaryExpression(
							BoolType.INSTANCE,
							variable,
							varAux,
							ComparisonGe.INSTANCE,
							pp.getLocation()),
					pp,
					pp,
					oracle);
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
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start,
					new BinaryExpression(
							BoolType.INSTANCE,
							variable,
							varAux,
							ComparisonLt.INSTANCE,
							pp.getLocation()),
					pp,
					pp,
					oracle);
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
			ValueEnvironment<Interval> act = singleton.assumeBinaryExpression(start,
					new BinaryExpression(
							BoolType.INSTANCE,
							variable,
							varAux,
							ComparisonLe.INSTANCE,
							pp.getLocation()),
					pp,
					pp,
					oracle);
			assertEquals("assume(" + variable + " <= " + bound + ") did not return " + exp, exp, act);
		}
	}

	@Test(expected = InfiniteIterationException.class)
	public void testIteratorOnTopInterval() {
		Interval top = new Interval();

		for (Long l : top.interval)
			System.out.println(l);
	}

	@Test
	public void testIterator() throws SemanticException {
		Interval top = mk(-1, 2);
		List<Long> values = new ArrayList<>();
		for (Long l : top.interval)
			values.add(l);

		List<Long> expected = new ArrayList<>();
		expected.add((long) -1);
		expected.add((long) 0);
		expected.add((long) 1);
		expected.add((long) 2);
		assertEquals(values, expected);

	}
}
