package it.unive.lisa.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.binary.ValueComparison;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class NegationRemovalTest {

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	private static BinaryExpression bin(
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator operator) {
		return new BinaryExpression(Untyped.INSTANCE, left, right, operator, SyntheticLocation.INSTANCE);
	}

	private static UnaryExpression not(
			SymbolicExpression expression) {
		return new UnaryExpression(Untyped.INSTANCE, expression, LogicalNegation.INSTANCE, SyntheticLocation.INSTANCE);
	}

	private void assertComparisonNegated(
			BinaryOperator operator,
			BinaryOperator opposite) {
		Variable x = var("x");
		Variable y = var("y");

		BinaryExpression expr = bin(x, y, operator);
		UnaryExpression negated = not(expr);

		ValueExpression result = negated.removeNegations();
		assertTrue(result instanceof BinaryExpression, "Negation is not a binary expression");

		BinaryExpression actual = (BinaryExpression) result;
		assertSame(x, actual.getLeft(), "Sub-expression has been re-created");
		assertSame(y, actual.getRight(), "Sub-expression has been re-created");
		assertSame(opposite, actual.getOperator(), "Operator has not been negated");
	}

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

	@Test
	public void testNegatedComparisonNe() {
		assertComparisonNegated(ComparisonNe.INSTANCE, ComparisonEq.INSTANCE);
	}

	@Test
	public void testNegatedComparisonLt() {
		assertComparisonNegated(ComparisonLt.INSTANCE, ComparisonGe.INSTANCE);
	}

	@Test
	public void testNegatedComparisonLe() {
		assertComparisonNegated(ComparisonLe.INSTANCE, ComparisonGt.INSTANCE);
	}

	@Test
	public void testNegatedComparisonGt() {
		assertComparisonNegated(ComparisonGt.INSTANCE, ComparisonLe.INSTANCE);
	}

	@Test
	public void testNegatedComparisonGe() {
		assertComparisonNegated(ComparisonGe.INSTANCE, ComparisonLt.INSTANCE);
	}

	@Test
	public void testNegatedValueComparison() {
		Variable x = var("x");
		Variable y = var("y");

		BinaryExpression expr = bin(x, y, ValueComparison.INSTANCE);
		UnaryExpression negated = not(expr);

		ValueExpression result = negated.removeNegations();
		assertSame(negated, result, "Negated expression has been recreated");
	}

	@Test
	public void testNegatedConjunctionOfComparisons() {
		Variable a = var("a");
		Variable b = var("b");
		Variable c = var("c");
		Variable d = var("d");

		BinaryExpression conjunction = bin(bin(a, b, ComparisonEq.INSTANCE), bin(c, d, ComparisonLt.INSTANCE), LogicalAnd.INSTANCE);
		UnaryExpression negated = not(conjunction);

		BinaryExpression expected = bin(bin(a, b, ComparisonNe.INSTANCE), bin(c, d, ComparisonGe.INSTANCE), LogicalOr.INSTANCE);

		ValueExpression result = negated.removeNegations();
		assertTrue(result instanceof BinaryExpression, "Negation is not a binary expression");
		assertSame(LogicalOr.INSTANCE, ((BinaryExpression) result).getOperator(), "Conjunction has not been turned into a disjunction");
		assertEquals(expected, result, "Negated conjunction is different from expected (De Morgan's law)");
	}

	@Test
	public void testNegatedDisjunctionOfComparisons() {
		Variable a = var("a");
		Variable b = var("b");
		Variable c = var("c");
		Variable d = var("d");

		BinaryExpression disjunction = bin(bin(a, b, ComparisonEq.INSTANCE), bin(c, d, ComparisonLt.INSTANCE), LogicalOr.INSTANCE);
		UnaryExpression negated = not(disjunction);

		BinaryExpression expected = bin(bin(a, b, ComparisonNe.INSTANCE), bin(c, d, ComparisonGe.INSTANCE), LogicalAnd.INSTANCE);

		ValueExpression result = negated.removeNegations();
		assertTrue(result instanceof BinaryExpression, "Negation is not a binary expression");
		assertSame(LogicalAnd.INSTANCE, ((BinaryExpression) result).getOperator(), "Disjunction has not been turned into a conjunction");
		assertEquals(expected, result, "Negated disjunction is different from expected (De Morgan's law)");
	}

	@Test
	public void testNegatedConjunctionWithNonNegatableOperand() {
		Variable a = var("a");
		Variable b = var("b");
		Variable x = var("x");
		Variable y = var("y");

		BinaryExpression comparison = bin(a, b, ComparisonEq.INSTANCE);
		BinaryExpression typeCheck = bin(x, y, TypeCheck.INSTANCE);
		BinaryExpression conjunction = bin(comparison, typeCheck, LogicalAnd.INSTANCE);
		UnaryExpression negated = not(conjunction);

		ValueExpression result = negated.removeNegations();
		assertTrue(result instanceof BinaryExpression, "Negation is not a binary expression");

		BinaryExpression actual = (BinaryExpression) result;
		assertSame(LogicalOr.INSTANCE, actual.getOperator(), "Conjunction has not been turned into a disjunction");
		assertEquals(bin(a, b, ComparisonNe.INSTANCE), actual.getLeft(), "Negatable operand has not been negated");
		assertTrue(actual.getRight() instanceof UnaryExpression,
				"Non-negatable operand should remain wrapped in a negation rather than being dropped");

		UnaryExpression negatedRight = (UnaryExpression) actual.getRight();
		assertSame(LogicalNegation.INSTANCE, negatedRight.getOperator());
		assertEquals(typeCheck, negatedRight.getExpression(), "Non-negatable operand has been altered");
	}

	@Test
	public void testNegatedNestedLogicalExpression() {
		Variable a = var("a");
		Variable b = var("b");
		Variable c = var("c");
		Variable d = var("d");
		Variable e = var("e");
		Variable f = var("f");

		BinaryExpression inner = bin(bin(a, b, ComparisonEq.INSTANCE), bin(c, d, ComparisonLt.INSTANCE), LogicalAnd.INSTANCE);
		BinaryExpression outer = bin(inner, bin(e, f, ComparisonGt.INSTANCE), LogicalOr.INSTANCE);
		UnaryExpression negated = not(outer);

		BinaryExpression expectedInner = bin(bin(a, b, ComparisonNe.INSTANCE), bin(c, d, ComparisonGe.INSTANCE), LogicalOr.INSTANCE);
		BinaryExpression expected = bin(expectedInner, bin(e, f, ComparisonLe.INSTANCE), LogicalAnd.INSTANCE);

		ValueExpression result = negated.removeNegations();
		assertEquals(expected, result, "Nested negation was not fully pushed down through both levels (De Morgan's law)");
	}

	@Test
	public void testDoubleNegationOfComparison() {
		Variable x = var("x");
		Variable y = var("y");

		BinaryExpression expr = bin(x, y, ComparisonEq.INSTANCE);
		UnaryExpression innerNegation = not(expr);
		UnaryExpression doubleNegation = not(innerNegation);

		ValueExpression result = doubleNegation.removeNegations();
		assertEquals(expr, result, "Double negation of a comparison was not eliminated");
	}

	@Test
	public void testNegatedVariableIsUnchanged() {
		Variable x = var("x");
		UnaryExpression negated = not(x);

		ValueExpression result = negated.removeNegations();
		assertSame(negated, result, "Negation of an atomic, non-binary expression should not be altered");
	}

	@Test
	public void testNegatedConstantIsUnchanged() {
		Constant c = new Constant(Untyped.INSTANCE, Boolean.TRUE, SyntheticLocation.INSTANCE);
		UnaryExpression negated = not(c);

		ValueExpression result = negated.removeNegations();
		assertSame(negated, result, "Negation of a constant should not be altered");
	}

	@Test
	public void testPlainComparisonIsIdentity() {
		BinaryExpression expr = bin(var("x"), var("y"), ComparisonEq.INSTANCE);
		assertSame(expr, expr.removeNegations(), "A non-negated comparison should be returned unchanged");
	}

	@Test
	public void testPlainConjunctionIsIdentity() {
		BinaryExpression conjunction = bin(
				bin(var("a"), var("b"), ComparisonEq.INSTANCE),
				bin(var("c"), var("d"), ComparisonLt.INSTANCE),
				LogicalAnd.INSTANCE);
		assertSame(conjunction, conjunction.removeNegations(), "A conjunction that is not itself negated should be returned unchanged");
	}

	@Test
	public void testVariableIsIdentity() {
		Variable x = var("x");
		assertSame(x, x.removeNegations(), "A variable should be returned unchanged");
	}

	@Test
	public void testConstantIsIdentity() {
		Constant c = new Constant(Untyped.INSTANCE, 5, SyntheticLocation.INSTANCE);
		assertSame(c, c.removeNegations(), "A constant should be returned unchanged");
	}

	@Test
	public void testNonLogicalUnaryOperatorIsUnaffected() {
		BinaryExpression expr = bin(var("x"), var("y"), ComparisonEq.INSTANCE);
		UnaryExpression arithmeticNegation = new UnaryExpression(
				Untyped.INSTANCE,
				expr,
				NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueExpression result = arithmeticNegation.removeNegations();
		assertSame(arithmeticNegation, result, "Non-logical unary operators should not trigger negation removal");
	}

}
