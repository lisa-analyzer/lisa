package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class BinaryExpressionTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	private static BinaryExpression bin(
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator op) {
		return new BinaryExpression(Untyped.INSTANCE, left, right, op, SyntheticLocation.INSTANCE);
	}

	@Test
	public void gettersReturnConstructorArguments() {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression e = bin(x, y, ComparisonEq.INSTANCE);
		assertSame(x, e.getLeft());
		assertSame(y, e.getRight());
		assertSame(ComparisonEq.INSTANCE, e.getOperator());
	}

	@Test
	public void equalsComparesLeftOperatorAndRight() {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression a = bin(x, y, ComparisonEq.INSTANCE);
		BinaryExpression b = bin(x, y, ComparisonEq.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		assertNotEquals(a, bin(x, y, ComparisonNe.INSTANCE));
		assertNotEquals(a, bin(var("z"), y, ComparisonEq.INSTANCE));
		assertNotEquals(a, bin(x, var("z"), ComparisonEq.INSTANCE));
	}

	@Test
	public void toStringIsLeftOperatorRight() {
		BinaryExpression e = bin(var("x"), var("y"), ComparisonEq.INSTANCE);
		assertEquals("x == y", e.toString());
	}

	@Test
	public void withOperatorReplacesOnlyTheOperator() {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression e = bin(x, y, ComparisonEq.INSTANCE);
		BinaryExpression replaced = e.withOperator(ComparisonNe.INSTANCE);
		assertSame(x, replaced.getLeft());
		assertSame(y, replaced.getRight());
		assertSame(ComparisonNe.INSTANCE, replaced.getOperator());
	}

	@Test
	public void mightNeedRewritingIsTrueIfEitherOperandDoes() throws Exception {
		// Untyped variables might need rewriting (Identifier#mightNeedRewriting
		// is true whenever the static type is untyped or not a value type),
		// so a BinaryExpression over two of them must report true too
		BinaryExpression e = bin(var("x"), var("y"), ComparisonEq.INSTANCE);
		assertEquals(true, e.mightNeedRewriting());

		it.unive.lisa.symbolic.value.Variable typed = new it.unive.lisa.symbolic.value.Variable(
				it.unive.lisa.type.VoidType.INSTANCE, "z", SyntheticLocation.INSTANCE);
		BinaryExpression allTyped = bin(typed, typed, ComparisonEq.INSTANCE);
		assertEquals(false, allTyped.mightNeedRewriting());
	}

	@Test
	public void acceptVisitsLeftThenRightThenItself() throws Exception {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression e = bin(x, y, ComparisonEq.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = e.accept(visitor);
		assertEquals(java.util.Arrays.asList(x, y, e), visitor.visited);
		assertSame(e, result);
	}

	@Test
	public void negateOnAComparisonFlipsOnlyTheOperator() {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression e = bin(x, y, ComparisonEq.INSTANCE);
		ValueExpression negated = e.negate();
		assertTrue(negated instanceof BinaryExpression);
		BinaryExpression b = (BinaryExpression) negated;
		assertSame(ComparisonNe.INSTANCE, b.getOperator());
		assertSame(x, b.getLeft());
		assertSame(y, b.getRight());
	}

	@Test
	public void negateOnALogicalOperatorAppliesDeMorgansLaw() {
		Variable a = var("a");
		Variable b = var("b");
		BinaryExpression conjunction = bin(a, b, LogicalAnd.INSTANCE);
		ValueExpression negated = conjunction.negate();
		assertTrue(negated instanceof BinaryExpression);
		BinaryExpression result = (BinaryExpression) negated;
		assertSame(LogicalOr.INSTANCE, result.getOperator());
		// De Morgan's law negates the operands too, not just the connective
		assertEquals(a.negate(), result.getLeft());
		assertEquals(b.negate(), result.getRight());
	}

	@Test
	public void negateOnANonNegatableOperatorFallsBackToWrappingInLogicalNegation() {
		BinaryExpression e = bin(var("x"), var("y"), TypeCheck.INSTANCE);
		ValueExpression negated = e.negate();
		assertTrue(negated instanceof UnaryExpression);
		assertSame(e, ((UnaryExpression) negated).getExpression());
	}

	@Test
	public void removeTypingExpressionsDropsTheRightOperandForTypeOperatorsOtherThanTypeCheck() {
		Variable x = var("x");
		BinaryExpression cast = bin(x, var("token"), TypeCast.INSTANCE);
		SymbolicExpression result = cast.removeTypingExpressions();
		assertSame(x, result);
	}

	@Test
	public void removeTypingExpressionsKeepsBothOperandsForTypeCheck() {
		Variable x = var("x");
		Variable token = var("token");
		BinaryExpression check = bin(x, token, TypeCheck.INSTANCE);
		assertSame(check, check.removeTypingExpressions());
	}

	@Test
	public void removeTypingExpressionsRecursesIntoBothOperandsForOrdinaryOperators() {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression e = bin(bin(x, y, ComparisonEq.INSTANCE), y, ComparisonEq.INSTANCE);
		assertSame(e, e.removeTypingExpressions());
	}

	@Test
	public void replaceSubstitutesTheWholeExpressionWhenItMatchesTheSource() {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression e = bin(x, y, ComparisonEq.INSTANCE);
		Variable target = var("z");
		assertSame(target, e.replace(e, target));
	}

	@Test
	public void replaceSubstitutesInsideEitherOperand() {
		Variable x = var("x");
		Variable y = var("y");
		Variable z = var("z");
		BinaryExpression e = bin(x, y, ComparisonEq.INSTANCE);
		SymbolicExpression replaced = e.replace(x, z);
		assertTrue(replaced instanceof BinaryExpression);
		assertSame(z, ((BinaryExpression) replaced).getLeft());
		assertSame(y, ((BinaryExpression) replaced).getRight());
	}

	@Test
	public void pushAndPopScopeAffectBothOperands() throws Exception {
		Variable x = var("x");
		Variable y = var("y");
		BinaryExpression e = bin(x, y, ComparisonEq.INSTANCE);

		SymbolicExpression pushed = e.pushScope(TOKEN, null);
		assertTrue(pushed instanceof BinaryExpression);
		BinaryExpression p = (BinaryExpression) pushed;
		assertTrue(p.getLeft() instanceof OutOfScopeIdentifier);
		assertTrue(p.getRight() instanceof OutOfScopeIdentifier);

		SymbolicExpression popped = p.popScope(TOKEN, null);
		assertEquals(e, popped);
	}

}
