package it.unive.lisa.symbolic.value.operator.binary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.OverflowingOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

// a light, table-driven sanity sweep for the leaf classes of NumericOperation
// and NumericComparison: none of them override typeInference, so the deep
// meaning-based coverage lives in NumericOperationTest/NumericComparisonTest;
// here we just verify each leaf's symbol, category marker, and (for the
// bit-width ones) overflow marker are wired correctly - a classic place for
// copy-paste slips given the sheer number of near-identical classes involved
public class NumericBinaryOperatorLeavesTest {

	private static class Expectation {

		final ArithmeticOperator instance;
		final String symbol;
		final boolean overflowing;

		Expectation(
				ArithmeticOperator instance,
				String symbol,
				boolean overflowing) {
			this.instance = instance;
			this.symbol = symbol;
			this.overflowing = overflowing;
		}
	}

	private static final List<Expectation> BIT_WIDTH_OPS = Arrays.asList(
			new Expectation(Numeric8BitAdd.INSTANCE, "+", true),
			new Expectation(Numeric8BitSub.INSTANCE, "-", true),
			new Expectation(Numeric8BitMul.INSTANCE, "*", true),
			new Expectation(Numeric8BitDiv.INSTANCE, "/", true),
			new Expectation(Numeric8BitMod.INSTANCE, "%", true),
			new Expectation(Numeric8BitRem.INSTANCE, "%", true),
			new Expectation(Numeric16BitAdd.INSTANCE, "+", true),
			new Expectation(Numeric16BitSub.INSTANCE, "-", true),
			new Expectation(Numeric16BitMul.INSTANCE, "*", true),
			new Expectation(Numeric16BitDiv.INSTANCE, "/", true),
			new Expectation(Numeric16BitMod.INSTANCE, "%", true),
			new Expectation(Numeric16BitRem.INSTANCE, "%", true),
			new Expectation(Numeric32BitAdd.INSTANCE, "+", true),
			new Expectation(Numeric32BitSub.INSTANCE, "-", true),
			new Expectation(Numeric32BitMul.INSTANCE, "*", true),
			new Expectation(Numeric32BitDiv.INSTANCE, "/", true),
			new Expectation(Numeric32BitMod.INSTANCE, "%", true),
			new Expectation(Numeric32BitRem.INSTANCE, "%", true),
			new Expectation(Numeric64BitAdd.INSTANCE, "+", true),
			new Expectation(Numeric64BitSub.INSTANCE, "-", true),
			new Expectation(Numeric64BitMul.INSTANCE, "*", true),
			new Expectation(Numeric64BitDiv.INSTANCE, "/", true),
			new Expectation(Numeric64BitMod.INSTANCE, "%", true),
			new Expectation(Numeric64BitRem.INSTANCE, "%", true),
			new Expectation(NumericNonOverflowingAdd.INSTANCE, "+", false),
			new Expectation(NumericNonOverflowingSub.INSTANCE, "-", false),
			new Expectation(NumericNonOverflowingMul.INSTANCE, "*", false),
			new Expectation(NumericNonOverflowingDiv.INSTANCE, "/", false),
			new Expectation(NumericNonOverflowingMod.INSTANCE, "%", false),
			new Expectation(NumericNonOverflowingRem.INSTANCE, "%", false));

	@Test
	public void bitWidthAndNonOverflowingOperatorsHaveTheExpectedSymbolAndOverflowMarker() {
		for (Expectation e : BIT_WIDTH_OPS) {
			assertEquals(e.symbol, e.instance.toString(), e.instance.getClass().getSimpleName());
			assertEquals(
					e.overflowing,
					e.instance instanceof OverflowingOperator,
					e.instance.getClass().getSimpleName() + " overflow marker mismatch");
		}
	}

	@Test
	public void additionOperatorsAllImplementAdditionOperator() {
		assertTrue(Numeric8BitAdd.INSTANCE instanceof AdditionOperator);
		assertTrue(Numeric16BitAdd.INSTANCE instanceof AdditionOperator);
		assertTrue(Numeric32BitAdd.INSTANCE instanceof AdditionOperator);
		assertTrue(Numeric64BitAdd.INSTANCE instanceof AdditionOperator);
		assertTrue(NumericNonOverflowingAdd.INSTANCE instanceof AdditionOperator);
		assertFalse(Numeric32BitAdd.INSTANCE instanceof SubtractionOperator);
	}

	@Test
	public void subtractionOperatorsAllImplementSubtractionOperator() {
		assertTrue(Numeric8BitSub.INSTANCE instanceof SubtractionOperator);
		assertTrue(Numeric32BitSub.INSTANCE instanceof SubtractionOperator);
		assertTrue(NumericNonOverflowingSub.INSTANCE instanceof SubtractionOperator);
	}

	@Test
	public void multiplicationOperatorsAllImplementMultiplicationOperator() {
		assertTrue(Numeric8BitMul.INSTANCE instanceof MultiplicationOperator);
		assertTrue(Numeric32BitMul.INSTANCE instanceof MultiplicationOperator);
		assertTrue(NumericNonOverflowingMul.INSTANCE instanceof MultiplicationOperator);
	}

	@Test
	public void divisionOperatorsAllImplementDivisionOperator() {
		assertTrue(Numeric8BitDiv.INSTANCE instanceof DivisionOperator);
		assertTrue(Numeric32BitDiv.INSTANCE instanceof DivisionOperator);
		assertTrue(NumericNonOverflowingDiv.INSTANCE instanceof DivisionOperator);
	}

	@Test
	public void moduloOperatorsImplementModuloOperatorAndNotRemainderOperator() {
		// Mod and Rem render identically ("%") but represent distinct
		// mathematical operations (Euclidean modulo vs. truncated-division
		// remainder, see each class's javadoc): their category markers must
		// stay distinct even though their concrete syntax does not
		assertTrue(Numeric32BitMod.INSTANCE instanceof ModuloOperator);
		assertFalse(Numeric32BitMod.INSTANCE instanceof RemainderOperator);
	}

	@Test
	public void remainderOperatorsImplementRemainderOperatorAndNotModuloOperator() {
		assertTrue(Numeric32BitRem.INSTANCE instanceof RemainderOperator);
		assertFalse(Numeric32BitRem.INSTANCE instanceof ModuloOperator);
	}

	@Test
	public void modAndRemAreDistinctOperatorsDespiteTheSameSymbol() {
		assertFalse(Numeric32BitMod.INSTANCE.equals(Numeric32BitRem.INSTANCE));
	}

	@Test
	public void miscNumericOperationsHaveTheExpectedSymbolAndAreNotOverflowing() {
		assertEquals("max", NumericMax.INSTANCE.toString());
		assertEquals("min", NumericMin.INSTANCE.toString());
		assertEquals("atan2", NumericAtan2.INSTANCE.toString());
		assertEquals("pow", NumericPow.INSTANCE.toString());
		assertFalse(NumericMax.INSTANCE instanceof OverflowingOperator);
		assertFalse(NumericMin.INSTANCE instanceof OverflowingOperator);
		assertFalse(NumericAtan2.INSTANCE instanceof OverflowingOperator);
		assertFalse(NumericPow.INSTANCE instanceof OverflowingOperator);
	}

	@Test
	public void comparisonOperatorsHaveTheExpectedSymbol() {
		assertEquals(">=", ComparisonGe.INSTANCE.toString());
		assertEquals(">", ComparisonGt.INSTANCE.toString());
		assertEquals("<=", ComparisonLe.INSTANCE.toString());
		assertEquals("<", ComparisonLt.INSTANCE.toString());
	}

	@Test
	public void comparisonOperatorsHaveTheExpectedMutuallyConsistentOpposites() {
		assertSame(ComparisonLt.INSTANCE, ComparisonGe.INSTANCE.opposite());
		assertSame(ComparisonGe.INSTANCE, ComparisonLt.INSTANCE.opposite());
		assertSame(ComparisonLe.INSTANCE, ComparisonGt.INSTANCE.opposite());
		assertSame(ComparisonGt.INSTANCE, ComparisonLe.INSTANCE.opposite());

		// opposite() must be an involution: applying it twice returns the
		// original operator
		assertSame(ComparisonGe.INSTANCE, ComparisonGe.INSTANCE.opposite().opposite());
		assertSame(ComparisonGt.INSTANCE, ComparisonGt.INSTANCE.opposite().opposite());
	}

}
