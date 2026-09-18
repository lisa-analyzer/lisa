package it.unive.lisa.symbolic.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DynamicAccess} ({@code p[s]}), the indexed-access half of
 * the former {@code AccessChild}, where the child is an arbitrary expression
 * computed at runtime.
 */
public class DynamicAccessTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	@Test
	public void gettersReturnConstructorArguments() {
		Variable container = var("arr");
		Variable child = var("i");
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		assertSame(container, a.getContainer());
		assertSame(child, a.getChild());
	}

	@Test
	public void toStringIsContainerArrowBracketChild() {
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, var("arr"), var("i"), SyntheticLocation.INSTANCE);
		assertEquals("arr->[i]", a.toString());
	}

	@Test
	public void equalsComparesContainerAndChild() {
		Variable container = var("arr");
		Variable child = var("i");
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		DynamicAccess b = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		assertFalse(a.equals(new DynamicAccess(Untyped.INSTANCE, var("other"), child, SyntheticLocation.INSTANCE)));
		assertFalse(a.equals(new DynamicAccess(Untyped.INSTANCE, container, var("other"), SyntheticLocation.INSTANCE)));
	}

	@Test
	public void mightNeedRewritingIsAlwaysTrue() {
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, var("arr"), var("i"), SyntheticLocation.INSTANCE);
		assertTrue(a.mightNeedRewriting());
	}

	@Test
	public void acceptVisitsContainerThenChildThenItself() throws Exception {
		Variable container = var("arr");
		Variable child = var("i");
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = a.accept(visitor);
		assertEquals(java.util.Arrays.asList(container, child, a), visitor.visited);
		assertSame(a, result);
	}

	@Test
	public void removeTypingExpressionsRecursesIntoBothOperands() {
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, var("arr"), var("i"), SyntheticLocation.INSTANCE);
		assertSame(a, a.removeTypingExpressions());
	}

	@Test
	public void replaceRecursesIntoBothOperands() {
		Variable container = var("arr");
		Variable child = var("i");
		Variable replacement = var("j");
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);

		SymbolicExpression replacedChild = a.replace(child, replacement);
		assertTrue(replacedChild instanceof DynamicAccess);
		assertSame(replacement, ((DynamicAccess) replacedChild).getChild());
		assertSame(container, ((DynamicAccess) replacedChild).getContainer());

		SymbolicExpression replacedContainer = a.replace(container, replacement);
		assertTrue(replacedContainer instanceof DynamicAccess);
		assertSame(replacement, ((DynamicAccess) replacedContainer).getContainer());
	}

	// characterization test for a known, deliberately-not-yet-fixed
	// limitation (see the FIXME on AccessChild#pushScope/#popScope):
	// pushScope/popScope only propagate through the container, silently
	// leaving the child (here, the index expression) untouched, even though
	// it can be an arbitrary scoped expression. This area is planned to
	// change soon; update this test when that happens.
	@Test
	public void pushScopeCurrentlyOnlyAffectsTheContainerNotTheChild() throws Exception {
		Variable container = var("arr");
		Variable child = var("i");
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);

		SymbolicExpression pushed = a.pushScope(TOKEN, null);
		assertTrue(pushed instanceof DynamicAccess);
		DynamicAccess p = (DynamicAccess) pushed;
		assertTrue(p.getContainer() instanceof OutOfScopeIdentifier);
		assertSame(child, p.getChild());
	}

	@Test
	public void popScopeReversesPushScopeOnTheContainer() throws Exception {
		Variable container = var("arr");
		Variable child = var("i");
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);

		SymbolicExpression pushed = a.pushScope(TOKEN, null);
		SymbolicExpression popped = ((DynamicAccess) pushed).popScope(TOKEN, null);
		assertEquals(a, popped);
	}

	@Test
	public void popScopePropagatesNullWhenTheContainerCannotBeScoped() throws Exception {
		// popScope on a plain Variable that was never pushed always yields
		// null; this must short-circuit the whole access to null too
		Variable container = var("arr");
		Variable child = var("i");
		DynamicAccess a = new DynamicAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		assertNull(a.popScope(TOKEN, null));
	}

}
