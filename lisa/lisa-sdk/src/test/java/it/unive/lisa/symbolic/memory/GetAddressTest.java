package it.unive.lisa.symbolic.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class GetAddressTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	@Test
	public void getExpressionReturnsTheConstructorArgument() {
		Variable x = var("x");
		GetAddress r = new GetAddress(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		assertSame(x, r.getExpression());
	}

	@Test
	public void toStringIsAmpersandInnerExpression() {
		GetAddress r = new GetAddress(Untyped.INSTANCE, var("x"), SyntheticLocation.INSTANCE);
		assertEquals("&x", r.toString());
	}

	@Test
	public void equalsComparesTypeAndInnerExpression() {
		Variable x = var("x");
		GetAddress a = new GetAddress(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		GetAddress b = new GetAddress(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new GetAddress(Untyped.INSTANCE, var("y"), SyntheticLocation.INSTANCE)));
	}

	@Test
	public void mightNeedRewritingIsAlwaysTrue() {
		assertTrue(new GetAddress(Untyped.INSTANCE, var("x"), SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

	@Test
	public void acceptVisitsTheInnerExpressionFirstThenItself() throws Exception {
		Variable x = var("x");
		GetAddress r = new GetAddress(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = r.accept(visitor);
		assertEquals(java.util.Arrays.asList(x, r), visitor.visited);
		assertSame(r, result);
	}

	@Test
	public void replaceSubstitutesTheWholeExpressionOrRecursesInside() {
		Variable x = var("x");
		Variable y = var("y");
		GetAddress r = new GetAddress(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		Variable target = var("z");
		assertSame(target, r.replace(r, target));

		SymbolicExpression replaced = r.replace(x, y);
		assertTrue(replaced instanceof GetAddress);
		assertSame(y, ((GetAddress) replaced).getExpression());
	}

	@Test
	public void pushScopeWrapsTheInnerExpressionWhenItChanges() throws Exception {
		Variable x = var("x");
		GetAddress r = new GetAddress(Untyped.INSTANCE, x, SyntheticLocation.INSTANCE);
		SymbolicExpression pushed = r.pushScope(TOKEN, null);
		assertTrue(pushed instanceof GetAddress);
		assertTrue(
				((GetAddress) pushed).getExpression() instanceof it.unive.lisa.symbolic.value.OutOfScopeIdentifier);
	}

}
