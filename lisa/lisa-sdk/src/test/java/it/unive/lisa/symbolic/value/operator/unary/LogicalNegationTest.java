package it.unive.lisa.symbolic.value.operator.unary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.LogicalOperator;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.type.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class LogicalNegationTest {

	private static final FixtureTypeSystem TS = new FixtureTypeSystem();

	@Test
	public void isALogicalNegatableOperatorAndPrintsBang() {
		assertTrue(LogicalNegation.INSTANCE instanceof LogicalOperator);
		assertTrue(LogicalNegation.INSTANCE instanceof NegatableOperator);
		assertEquals("!", LogicalNegation.INSTANCE.toString());
	}

	@Test
	public void isItsOwnOpposite() {
		// double negation cancels out: opposite(opposite(!)) must be ! again,
		// which trivially holds if opposite(!) == ! (there is no other unary
		// logical operator for it to map to)
		assertSame(LogicalNegation.INSTANCE, LogicalNegation.INSTANCE.opposite());
	}

	@Test
	public void typeInferenceKeepsOnlyBooleanArgumentsAndReturnsBoolean() {
		Set<Type> mixed = new HashSet<>(Arrays.asList(FixtureTypeSystem.BOOL, FixtureTypeSystem.INT));
		assertEquals(
				Collections.singleton(FixtureTypeSystem.BOOL),
				LogicalNegation.INSTANCE.typeInference(TS, mixed));
	}

	@Test
	public void typeInferenceIsEmptyWhenArgumentHasNoBooleanType() {
		Set<Type> nonBoolean = new HashSet<>(Arrays.asList(FixtureTypeSystem.INT, FixtureTypeSystem.STR));
		assertTrue(LogicalNegation.INSTANCE.typeInference(TS, nonBoolean).isEmpty());
	}

}
