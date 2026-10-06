package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class UnaryExpressionTest {

	// CodeElement is a @FunctionalInterface with a single getLocation()
	// method, so a lambda is a legitimate minimal fake for a ScopeToken
	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	@Test
	public void gettersReturnConstructorArguments() {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		assertSame(x, e.getExpression());
		assertSame(NumericNegation.INSTANCE, e.getOperator());
	}

	@Test
	public void equalsComparesTypeOperatorAndInnerExpression() {
		Variable x = var("x");
		UnaryExpression a = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		UnaryExpression b = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				new SourceCodeLocation("f", 1, 0));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		UnaryExpression differentOperator = new UnaryExpression(
				Untyped.INSTANCE, x, LogicalNegation.INSTANCE, SyntheticLocation.INSTANCE);
		assertNotEquals(a, differentOperator);

		UnaryExpression differentInner = new UnaryExpression(
				Untyped.INSTANCE, var("y"), NumericNegation.INSTANCE, SyntheticLocation.INSTANCE);
		assertNotEquals(a, differentInner);
	}

	@Test
	public void toStringIsOperatorFollowedByExpression() {
		UnaryExpression e = new UnaryExpression(
				Untyped.INSTANCE, var("x"), NumericNegation.INSTANCE, SyntheticLocation.INSTANCE);
		assertEquals("- x", e.toString());
	}

	@Test
	public void withOperatorReplacesOnlyTheOperator() {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		UnaryExpression replaced = e.withOperator(LogicalNegation.INSTANCE);
		assertSame(x, replaced.getExpression());
		assertSame(LogicalNegation.INSTANCE, replaced.getOperator());
		assertSame(e.getStaticType(), replaced.getStaticType());
	}

	@Test
	public void mightNeedRewritingDelegatesToTheInnerExpression() throws Exception {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		assertEquals(x.mightNeedRewriting(), e.mightNeedRewriting());
	}

	@Test
	public void acceptVisitsTheInnerExpressionFirstThenItself() throws Exception {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = e.accept(visitor);
		assertEquals(java.util.Arrays.asList(x, e), visitor.visited);
		assertSame(e, result);
	}

	@Test
	public void removeTypingExpressionsPreservesReferenceWhenNothingChanges() {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		assertSame(e, e.removeTypingExpressions());
	}

	@Test
	public void replaceSubstitutesTheWholeExpressionWhenItMatchesTheSource() {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		Variable target = var("y");
		assertSame(target, e.replace(e, target));
	}

	@Test
	public void replaceSubstitutesInsideTheInnerExpression() {
		Variable x = var("x");
		Variable y = var("y");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		SymbolicExpression replaced = e.replace(x, y);
		assertTrue(replaced instanceof UnaryExpression);
		assertSame(y, ((UnaryExpression) replaced).getExpression());
	}

	@Test
	public void replaceReturnsSameReferenceWhenSourceIsNotFound() {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);
		assertSame(e, e.replace(var("z"), var("w")));
	}

	@Test
	public void pushAndPopScopeDelegateToTheInnerExpression() throws Exception {
		Variable x = var("x");
		UnaryExpression e = new UnaryExpression(Untyped.INSTANCE, x, NumericNegation.INSTANCE,
				SyntheticLocation.INSTANCE);

		SymbolicExpression pushed = e.pushScope(TOKEN, null);
		assertTrue(pushed instanceof UnaryExpression);
		assertTrue(((UnaryExpression) pushed).getExpression() instanceof OutOfScopeIdentifier);

		SymbolicExpression popped = ((UnaryExpression) pushed).popScope(TOKEN, null);
		assertEquals(e, popped);
	}

}
