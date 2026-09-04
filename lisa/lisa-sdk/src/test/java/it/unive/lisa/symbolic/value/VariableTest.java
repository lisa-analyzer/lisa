package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class VariableTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void canBeScopedIsTrue() {
		assertTrue(new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE).canBeScoped());
	}

	@Test
	public void pushScopeWrapsInAnOutOfScopeIdentifier() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		SymbolicExpression pushed = v.pushScope(TOKEN, null);
		assertTrue(pushed instanceof OutOfScopeIdentifier);
		OutOfScopeIdentifier o = (OutOfScopeIdentifier) pushed;
		assertSame(TOKEN, o.getScope());
		assertEquals(TOKEN + ":x", o.getName());
	}

	@Test
	public void popScopeOnAPlainVariableIsAlwaysNull() throws Exception {
		// per SymbolicExpression#popScope's contract: a not-yet-scoped
		// identifier has nothing to pop
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertNull(v.popScope(TOKEN, null));
	}

	@Test
	public void toStringIsTheName() {
		assertEquals("x", new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE).toString());
	}

	@Test
	public void acceptDispatchesToTheIdentifierOverload() throws Exception {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		v.accept(visitor);
		assertEquals(java.util.Collections.singletonList(v), visitor.visited);
	}

	@Test
	public void replaceSubstitutesWhenItMatchesTheSourceOtherwiseIdentity() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		Variable target = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);
		assertSame(target, v.replace(v, target));
		assertSame(v, v.replace(target, v));
	}

}
