package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class ConstantTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void getValueReturnsTheConstructorArgument() {
		Constant c = new Constant(Untyped.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertEquals(42, c.getValue());
	}

	@Test
	public void equalsComparesTypeAndValue() {
		Constant a = new Constant(Untyped.INSTANCE, 42, SyntheticLocation.INSTANCE);
		Constant b = new Constant(Untyped.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		assertFalse(a.equals(new Constant(Untyped.INSTANCE, 43, SyntheticLocation.INSTANCE)));
		assertFalse(a.equals(new Constant(VoidType.INSTANCE, 42, SyntheticLocation.INSTANCE)));
	}

	@Test
	public void toStringQuotesStringValuesButNotOthers() {
		assertEquals("\"hi\"", new Constant(Untyped.INSTANCE, "hi", SyntheticLocation.INSTANCE).toString());
		assertEquals("42", new Constant(Untyped.INSTANCE, 42, SyntheticLocation.INSTANCE).toString());
		assertEquals("true", new Constant(Untyped.INSTANCE, true, SyntheticLocation.INSTANCE).toString());
	}

	@Test
	public void pushAndPopScopeAreNoOps() throws Exception {
		Constant c = new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE);
		assertSame(c, c.pushScope(TOKEN, null));
		assertSame(c, c.popScope(TOKEN, null));
	}

	@Test
	public void mightNeedRewritingFollowsTheStaticTypeRules() {
		assertFalse(new Constant(VoidType.INSTANCE, 1, SyntheticLocation.INSTANCE).mightNeedRewriting());
		assertTrue(new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE).mightNeedRewriting());
		assertTrue(new Constant(NullType.INSTANCE, 1, SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

	@Test
	public void removeTypingExpressionsIsIdentity() {
		Constant c = new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE);
		assertSame(c, c.removeTypingExpressions());
	}

	@Test
	public void acceptDispatchesToTheConstantOverload() throws Exception {
		Constant c = new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = c.accept(visitor);
		assertSame(c, result);
		assertEquals(java.util.Collections.singletonList(c), visitor.visited);
	}

	@Test
	public void replaceSubstitutesWhenItMatchesTheSourceOtherwiseIdentity() {
		Constant c = new Constant(Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE);
		Constant target = new Constant(Untyped.INSTANCE, 2, SyntheticLocation.INSTANCE);
		assertSame(target, c.replace(c, target));
		assertSame(c, c.replace(target, c));
	}

}
