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
 * Tests for {@link StaticAccess} ({@code p.f}), the field-access half of the
 * former {@code AccessChild}, where the child is the (constant) name of the
 * field being accessed.
 */
public class StaticAccessTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private static Variable var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	@Test
	public void gettersReturnConstructorArguments() {
		Variable container = var("arr");
		Variable child = var("f");
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		assertSame(container, a.getContainer());
		assertSame(child, a.getChild());
	}

	@Test
	public void toStringIsContainerArrowChild() {
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, var("arr"), var("f"), SyntheticLocation.INSTANCE);
		assertEquals("arr->f", a.toString());
	}

	@Test
	public void equalsComparesContainerAndChild() {
		Variable container = var("arr");
		Variable child = var("f");
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		StaticAccess b = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		assertFalse(a.equals(new StaticAccess(Untyped.INSTANCE, var("other"), child, SyntheticLocation.INSTANCE)));
		assertFalse(a.equals(new StaticAccess(Untyped.INSTANCE, container, var("other"), SyntheticLocation.INSTANCE)));
	}

	@Test
	public void mightNeedRewritingIsAlwaysTrue() {
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, var("arr"), var("f"), SyntheticLocation.INSTANCE);
		assertTrue(a.mightNeedRewriting());
	}

	@Test
	public void acceptVisitsTheContainerThenItself() throws Exception {
		Variable container = var("arr");
		Variable child = var("f");
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = a.accept(visitor);
		// the field name is a constant handed directly to the callback, so it
		// is not itself visited: only the container is traversed
		assertEquals(java.util.Arrays.asList(container, a), visitor.visited);
		assertSame(a, result);
	}

	@Test
	public void removeTypingExpressionsRecursesIntoTheContainer() {
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, var("arr"), var("f"), SyntheticLocation.INSTANCE);
		assertSame(a, a.removeTypingExpressions());
	}

	@Test
	public void replaceRecursesIntoTheContainerButNotTheConstantField() {
		Variable container = var("arr");
		Variable child = var("f");
		Variable replacement = var("j");
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);

		// the field name is constant and is never substituted
		assertSame(a, a.replace(child, replacement));

		SymbolicExpression replacedContainer = a.replace(container, replacement);
		assertTrue(replacedContainer instanceof StaticAccess);
		assertSame(replacement, ((StaticAccess) replacedContainer).getContainer());
		assertSame(child, ((StaticAccess) replacedContainer).getChild());
	}

	// characterization test for a known, deliberately-not-yet-fixed
	// limitation (see the FIXME on AccessChild#pushScope/#popScope):
	// pushScope/popScope only propagate through the container, silently
	// leaving the child untouched.
	@Test
	public void pushScopeCurrentlyOnlyAffectsTheContainerNotTheChild() throws Exception {
		Variable container = var("arr");
		Variable child = var("f");
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);

		SymbolicExpression pushed = a.pushScope(TOKEN, null);
		assertTrue(pushed instanceof StaticAccess);
		StaticAccess p = (StaticAccess) pushed;
		assertTrue(p.getContainer() instanceof OutOfScopeIdentifier);
		assertSame(child, p.getChild());
	}

	@Test
	public void popScopeReversesPushScopeOnTheContainer() throws Exception {
		Variable container = var("arr");
		Variable child = var("f");
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);

		SymbolicExpression pushed = a.pushScope(TOKEN, null);
		SymbolicExpression popped = ((StaticAccess) pushed).popScope(TOKEN, null);
		assertEquals(a, popped);
	}

	@Test
	public void popScopePropagatesNullWhenTheContainerCannotBeScoped() throws Exception {
		// popScope on a plain Variable that was never pushed always yields
		// null; this must short-circuit the whole access to null too
		Variable container = var("arr");
		Variable child = var("f");
		StaticAccess a = new StaticAccess(Untyped.INSTANCE, container, child, SyntheticLocation.INSTANCE);
		assertNull(a.popScope(TOKEN, null));
	}

}
