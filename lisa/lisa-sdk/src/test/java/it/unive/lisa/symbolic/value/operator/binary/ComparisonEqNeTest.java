package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.BOOL;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.INT32;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ComparisonEqNeTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	@Test
	public void toStringIsTheirSymbol() {
		assertEquals("==", ComparisonEq.INSTANCE.toString());
		assertEquals("!=", ComparisonNe.INSTANCE.toString());
	}

	@Test
	public void oppositeOfEqIsNeAndViceVersa() {
		ComparisonOperator eqOpp = ComparisonEq.INSTANCE.opposite();
		ComparisonOperator neOpp = ComparisonNe.INSTANCE.opposite();
		assertSame(ComparisonNe.INSTANCE, eqOpp);
		assertSame(ComparisonEq.INSTANCE, neOpp);
	}

	@Test
	public void typeInferenceAlwaysYieldsBooleanRegardlessOfOperandTypes() {
		// equality/inequality is documented to work on "any Type", unlike
		// the numeric/character-restricted comparisons in this package
		Set<Type> expected = Collections.singleton(BOOL);
		assertEquals(expected,
				ComparisonEq.INSTANCE.typeInference(TS, Collections.singleton(STR), Collections.singleton(INT32)));
		assertEquals(expected, ComparisonNe.INSTANCE.typeInference(TS, Collections.emptySet(), Collections.emptySet()));
	}

}
