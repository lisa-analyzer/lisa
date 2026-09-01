package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.BOOL;
import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.STR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class LogicalOperatorsTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	@Test
	public void typeInferenceRequiresBooleanOnBothSides() {
		Set<Type> bools = Collections.singleton(BOOL);
		Set<Type> other = Collections.singleton(STR);

		assertEquals(Collections.singleton(BOOL), LogicalAnd.INSTANCE.typeInference(TS, bools, bools));
		assertTrue(LogicalAnd.INSTANCE.typeInference(TS, bools, other).isEmpty());
		assertTrue(LogicalAnd.INSTANCE.typeInference(TS, other, bools).isEmpty());
		assertTrue(LogicalOr.INSTANCE.typeInference(TS, other, other).isEmpty());
	}

	@Test
	public void toStringIsTheirSymbol() {
		assertEquals("&&", LogicalAnd.INSTANCE.toString());
		assertEquals("||", LogicalOr.INSTANCE.toString());
	}

	@Test
	public void oppositeOfAndIsOrAndViceVersa() {
		LogicalOperator andOpp = LogicalAnd.INSTANCE.opposite();
		LogicalOperator orOpp = LogicalOr.INSTANCE.opposite();
		assertSame(LogicalOr.INSTANCE, andOpp);
		assertSame(LogicalAnd.INSTANCE, orOpp);
	}

}
