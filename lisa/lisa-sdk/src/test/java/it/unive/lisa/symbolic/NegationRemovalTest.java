package it.unive.lisa.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class NegationRemovalTest {

	@Test
	public void testNeagtedComparison() {
		Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		Variable y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

		BinaryExpression expr = new BinaryExpression(
				Untyped.INSTANCE,
				x,
				y,
				ComparisonEq.INSTANCE,
				SyntheticLocation.INSTANCE);

		UnaryExpression negated = new UnaryExpression(
				Untyped.INSTANCE,
				expr,
				LogicalNegation.INSTANCE,
				SyntheticLocation.INSTANCE);

		BinaryExpression expected = new BinaryExpression(
				Untyped.INSTANCE,
				x,
				y,
				ComparisonNe.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression result = negated.removeNegations();
		assertTrue(result instanceof BinaryExpression, "Negation is not a binary expression");

		BinaryExpression actual = (BinaryExpression) result;
		assertSame(x, actual.getLeft(), "Sub-expression has been re-created");
		assertSame(y, actual.getRight(), "Sub-expression has been re-created");
		assertSame(ComparisonNe.INSTANCE, actual.getOperator(), "Operator has not been negated");
		assertEquals(expected, actual, "Negated expression is different from expected");
	}

	@Test
	public void testNeagtedTypeCheck() {
		Variable x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		Variable y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

		BinaryExpression expr = new BinaryExpression(
				Untyped.INSTANCE,
				x,
				y,
				TypeCheck.INSTANCE,
				SyntheticLocation.INSTANCE);

		UnaryExpression negated = new UnaryExpression(
				Untyped.INSTANCE,
				expr,
				LogicalNegation.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression result = negated.removeNegations();
		assertSame(negated, result, "Negated expression has been recreated");
	}

}
