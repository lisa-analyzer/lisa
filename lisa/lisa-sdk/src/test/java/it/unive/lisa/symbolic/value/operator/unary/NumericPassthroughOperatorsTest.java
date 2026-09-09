package it.unive.lisa.symbolic.value.operator.unary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.symbolic.value.operator.ArithmeticOperator;
import it.unive.lisa.symbolic.value.operator.BitwiseOperator;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

// all of these operators share the exact same typeInference() behavior
// (filter down to the numeric types among the argument, or an empty set if
// none is numeric) despite each hand-writing its own copy of the logic, with
// no shared base class - this is exactly the shape where a copy-paste
// divergence is most likely, so every one of them gets the same real
// coverage rather than being assumed correct because it "looks boilerplate"
public class NumericPassthroughOperatorsTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	private static final Map<UnaryOperator, String> OPERATORS = new LinkedHashMap<>();
	static {
		OPERATORS.put(NumericAbs.INSTANCE, "abs");
		OPERATORS.put(NumericAcos.INSTANCE, "acos");
		OPERATORS.put(NumericAsin.INSTANCE, "asin");
		OPERATORS.put(NumericAtan.INSTANCE, "atan");
		OPERATORS.put(NumericCeil.INSTANCE, "ceil");
		OPERATORS.put(NumericCos.INSTANCE, "cos");
		OPERATORS.put(NumericExp.INSTANCE, "exp");
		OPERATORS.put(NumericFloor.INSTANCE, "floor");
		OPERATORS.put(NumericLog.INSTANCE, "log");
		OPERATORS.put(NumericLog10.INSTANCE, "log10");
		OPERATORS.put(NumericRound.INSTANCE, "round");
		OPERATORS.put(NumericSin.INSTANCE, "sin");
		OPERATORS.put(NumericSqrt.INSTANCE, "sqrt");
		OPERATORS.put(NumericTan.INSTANCE, "tan");
		OPERATORS.put(NumericToRadians.INSTANCE, "toRadians");
		OPERATORS.put(NumericNegation.INSTANCE, "-");
		OPERATORS.put(BitwiseNegation.INSTANCE, "~");
	}

	@Test
	public void singletonIsNotNullAndToStringMatches() {
		for (Map.Entry<UnaryOperator, String> entry : OPERATORS.entrySet()) {
			assertNotNull(entry.getKey());
			assertTrue(entry.getKey() instanceof Operator);
			assertEquals(entry.getValue(), entry.getKey().toString(), entry.getKey().getClass().getSimpleName());
		}
	}

	@Test
	public void isEitherArithmeticOrBitwise() {
		for (UnaryOperator op : OPERATORS.keySet())
			assertTrue(
					op instanceof ArithmeticOperator || op instanceof BitwiseOperator,
					op.getClass().getSimpleName());
	}

	@Test
	public void typeInferenceKeepsOnlyNumericTypesFromTheArgument() {
		Set<Type> mixed = new HashSet<>(Arrays.asList(FixtureTypeSystem.INT, FixtureTypeSystem.STR));
		for (UnaryOperator op : OPERATORS.keySet())
			assertEquals(
					Collections.singleton(FixtureTypeSystem.INT),
					op.typeInference(TS, mixed),
					op.getClass().getSimpleName());
	}

	@Test
	public void typeInferenceIsEmptyWhenArgumentHasNoNumericType() {
		Set<Type> nonNumeric = new HashSet<>(Arrays.asList(FixtureTypeSystem.STR, FixtureTypeSystem.BOOL));
		for (UnaryOperator op : OPERATORS.keySet())
			assertTrue(op.typeInference(TS, nonNumeric).isEmpty(), op.getClass().getSimpleName());
	}

	@Test
	public void typeInferenceIsEmptyOnAnEmptyArgument() {
		for (UnaryOperator op : OPERATORS.keySet())
			assertTrue(op.typeInference(TS, Collections.emptySet()).isEmpty(), op.getClass().getSimpleName());
	}

}
