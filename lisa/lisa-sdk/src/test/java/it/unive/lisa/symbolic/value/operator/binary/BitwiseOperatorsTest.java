package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.FLOAT32;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.INT32;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BitwiseOperatorsTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	// all six share NumericOperation as their common ancestor, which is
	// where typeInference actually lives
	private static final List<NumericOperation> OPERATORS = Arrays.asList(
			BitwiseAnd.INSTANCE,
			BitwiseOr.INSTANCE,
			BitwiseXor.INSTANCE,
			BitwiseShiftLeft.INSTANCE,
			BitwiseShiftRight.INSTANCE,
			BitwiseUnsignedShiftRight.INSTANCE);

	@Test
	public void toStringMatchesEachOperatorsSymbol() {
		assertEquals("&", BitwiseAnd.INSTANCE.toString());
		assertEquals("|", BitwiseOr.INSTANCE.toString());
		assertEquals("^", BitwiseXor.INSTANCE.toString());
		assertEquals("<<", BitwiseShiftLeft.INSTANCE.toString());
		assertEquals(">>", BitwiseShiftRight.INSTANCE.toString());
		assertEquals(">>>", BitwiseUnsignedShiftRight.INSTANCE.toString());
	}

	@Test
	public void typeInferenceRejectsNonNumericOperands() {
		Set<Type> numeric = Collections.singleton(INT32);
		Set<Type> other = Collections.singleton(STR);
		for (NumericOperation op : OPERATORS) {
			assertTrue(op.typeInference(TS, numeric, other).isEmpty(),
					op + ": should reject a non-numeric right operand");
			assertTrue(op.typeInference(TS, other, numeric).isEmpty(),
					op + ": should reject a non-numeric left operand");
		}
	}

	@Test
	public void typeInferenceAcceptsMatchingNumericOperands() {
		Set<Type> numeric = Collections.singleton(INT32);
		for (NumericOperation op : OPERATORS)
			assertEquals(numeric, op.typeInference(TS, numeric, numeric),
					op + ": should accept two matching integral operands");
	}

	@Test
	public void typeInferenceCurrentlyAcceptsNonIntegralNumericOperandsToo() {
		// documents current (permissive) behavior: these operators all
		// extend the generic NumericOperation base, which only requires
		// isNumericType() - not isIntegral() - on both sides, so a
		// floating-point operand is not rejected even though bitwise
		// operations are not conventionally meaningful on non-integral
		// values. This is consistent with each class's own javadoc, which
		// only ever documents "any NumericType" rather than "any integral
		// NumericType", so this is not a confirmed contract violation, but
		// it may be worth a stricter typeInference in the future.
		Set<Type> nonIntegral = Collections.singleton(FLOAT32);
		for (NumericOperation op : OPERATORS)
			assertEquals(nonIntegral, op.typeInference(TS, nonIntegral, nonIntegral),
					op + ": currently accepts non-integral operands");
	}

}
