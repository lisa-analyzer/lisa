package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class TernaryExpressionTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	private static TernaryExpression ter(
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right) {
		return new TernaryExpression(
				Untyped.INSTANCE, left, middle, right, StringSubstring.INSTANCE, SyntheticLocation.INSTANCE);
	}

	@Test
	public void gettersReturnConstructorArguments() {
		Variable x = var("x");
		Variable m = var("m");
		Variable y = var("y");
		TernaryExpression e = ter(x, m, y);
		assertSame(x, e.getLeft());
		assertSame(m, e.getMiddle());
		assertSame(y, e.getRight());
		assertSame(StringSubstring.INSTANCE, e.getOperator());
	}

	@Test
	public void equalsComparesAllThreeOperandsAndTheOperator() {
		Variable x = var("x");
		Variable m = var("m");
		Variable y = var("y");
		TernaryExpression a = ter(x, m, y);
		TernaryExpression b = ter(x, m, y);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		assertNotEquals(a, ter(var("z"), m, y));
		assertNotEquals(a, ter(x, var("z"), y));
		assertNotEquals(a, ter(x, m, var("z")));
	}

	@Test
	public void toStringIsLeftOperatorParenMiddleCommaRight() {
		TernaryExpression e = ter(var("x"), var("m"), var("y"));
		assertEquals("x " + StringSubstring.INSTANCE + "(m, y)", e.toString());
	}

	@Test
	public void withOperatorReplacesOnlyTheOperator() {
		Variable x = var("x");
		Variable m = var("m");
		Variable y = var("y");
		TernaryExpression e = ter(x, m, y);
		TernaryExpression replaced = e.withOperator(StringSubstring.INSTANCE);
		assertSame(x, replaced.getLeft());
		assertSame(m, replaced.getMiddle());
		assertSame(y, replaced.getRight());
	}

	@Test
	public void mightNeedRewritingIsTrueWhenAnyOperandIsUntyped() throws Exception {
		// Untyped variables might need rewriting (Identifier#mightNeedRewriting
		// is true whenever the static type is untyped or not a value type)
		TernaryExpression e = ter(var("x"), var("m"), var("y"));
		assertEquals(true, e.mightNeedRewriting());

		it.unive.lisa.symbolic.value.Variable typed = new it.unive.lisa.symbolic.value.Variable(
				it.unive.lisa.type.VoidType.INSTANCE, "z", SyntheticLocation.INSTANCE);
		TernaryExpression allTyped = ter(typed, typed, typed);
		assertEquals(false, allTyped.mightNeedRewriting());
	}

	@Test
	public void acceptVisitsLeftMiddleRightThenItself() throws Exception {
		Variable x = var("x");
		Variable m = var("m");
		Variable y = var("y");
		TernaryExpression e = ter(x, m, y);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = e.accept(visitor);
		assertEquals(java.util.Arrays.asList(x, m, y, e), visitor.visited);
		assertSame(e, result);
	}

	@Test
	public void removeTypingExpressionsPreservesReferenceWhenNothingChanges() {
		TernaryExpression e = ter(var("x"), var("m"), var("y"));
		assertSame(e, e.removeTypingExpressions());
	}

	@Test
	public void replaceSubstitutesTheWholeExpressionWhenItMatchesTheSource() {
		TernaryExpression e = ter(var("x"), var("m"), var("y"));
		Variable target = var("z");
		assertSame(target, e.replace(e, target));
	}

	@Test
	public void replaceSubstitutesInsideAnyOperand() {
		Variable m = var("m");
		Variable y = var("y");
		Variable x = var("x");
		Variable z = var("z");
		TernaryExpression e = ter(x, m, y);
		SymbolicExpression replaced = e.replace(m, z);
		assertTrue(replaced instanceof TernaryExpression);
		assertSame(z, ((TernaryExpression) replaced).getMiddle());
	}

	@Test
	public void pushAndPopScopeAffectAllThreeOperands() throws Exception {
		Variable x = var("x");
		Variable m = var("m");
		Variable y = var("y");
		TernaryExpression e = ter(x, m, y);

		SymbolicExpression pushed = e.pushScope(TOKEN, null);
		assertTrue(pushed instanceof TernaryExpression);
		TernaryExpression p = (TernaryExpression) pushed;
		assertTrue(p.getLeft() instanceof OutOfScopeIdentifier);
		assertTrue(p.getMiddle() instanceof OutOfScopeIdentifier);
		assertTrue(p.getRight() instanceof OutOfScopeIdentifier);

		SymbolicExpression popped = p.popScope(TOKEN, null);
		assertEquals(e, popped);
	}

}
