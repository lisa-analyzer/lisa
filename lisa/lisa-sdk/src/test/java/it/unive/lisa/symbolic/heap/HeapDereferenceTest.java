package it.unive.lisa.symbolic.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class HeapDereferenceTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	@Test
	public void getExpressionReturnsTheConstructorArgument() {
		Variable x = var("x");
		HeapDereference d = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		assertSame(x, d.getExpression());
	}

	@Test
	public void toStringWrapsTheInnerExpressionInStars() {
		HeapDereference d = new HeapDereference(Untyped.INSTANCE, var("x"), SyntheticLocation.INSTANCE);
		assertEquals("*(x)", d.toString());
	}

	@Test
	public void equalsComparesTypeAndInnerExpression() {
		Variable x = var("x");
		HeapDereference a = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		HeapDereference b = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new HeapDereference(Untyped.INSTANCE, var("y"), SyntheticLocation.INSTANCE)));
	}

	@Test
	public void mightNeedRewritingIsAlwaysTrue() {
		assertTrue(new HeapDereference(Untyped.INSTANCE, var("x"), SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

	@Test
	public void acceptVisitsTheInnerExpressionFirstThenItself() throws Exception {
		Variable x = var("x");
		HeapDereference d = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = d.accept(visitor);
		assertEquals(java.util.Arrays.asList(x, d), visitor.visited);
		assertSame(d, result);
	}

	@Test
	public void removeTypingExpressionsRecursesIntoTheInnerExpression() {
		Variable x = var("x");
		HeapDereference d = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		assertSame(d, d.removeTypingExpressions());
	}

	@Test
	public void replaceSubstitutesTheWholeExpressionOrRecursesInside() {
		Variable x = var("x");
		Variable y = var("y");
		HeapDereference d = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		Variable target = var("z");
		assertSame(target, d.replace(d, target));

		SymbolicExpression replaced = d.replace(x, y);
		assertTrue(replaced instanceof HeapDereference);
		assertSame(y, ((HeapDereference) replaced).getExpression());
	}

	@Test
	public void pushScopePropagatesNullFromTheInnerExpression() throws Exception {
		Variable x = var("x");
		HeapDereference d = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		// popScope on a plain (not-yet-scoped) Variable always yields null,
		// which must propagate through the dereference
		assertNull(d.popScope(TOKEN, null));
	}

	@Test
	public void pushScopeWrapsTheInnerExpressionWhenItChanges() throws Exception {
		Variable x = var("x");
		HeapDereference d = new HeapDereference(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		SymbolicExpression pushed = d.pushScope(TOKEN, null);
		assertTrue(pushed instanceof HeapDereference);
		assertTrue(((HeapDereference) pushed)
				.getExpression() instanceof it.unive.lisa.symbolic.value.OutOfScopeIdentifier);
	}

}
