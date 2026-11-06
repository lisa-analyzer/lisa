package it.unive.lisa.symbolic.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.NullType;
import org.junit.jupiter.api.Test;

public class NullConstantTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void staticTypeIsAlwaysNullType() {
		assertSame(NullType.INSTANCE, new NullConstant(SyntheticLocation.INSTANCE).getStaticType());
	}

	@Test
	public void toStringIsNull() {
		assertEquals("null", new NullConstant(SyntheticLocation.INSTANCE).toString());
	}

	@Test
	public void twoNullConstantsAreEqualRegardlessOfLocation() {
		NullConstant a = new NullConstant(SyntheticLocation.INSTANCE);
		NullConstant b = new NullConstant(new SourceCodeLocation("f", 1, 0));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void mightNeedRewritingIsAlwaysTrueBeingAHeapExpression() {
		// HeapExpression#mightNeedRewriting is final and always true: any
		// heap expression must be rewritten by a HeapDomain before a
		// ValueDomain can process it, per HeapExpression's own javadoc
		assertTrue(new NullConstant(SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

	@Test
	public void pushAndPopScopeAreNoOps() throws Exception {
		NullConstant n = new NullConstant(SyntheticLocation.INSTANCE);
		assertSame(n, n.pushScope(TOKEN, null));
		assertSame(n, n.popScope(TOKEN, null));
	}

	@Test
	public void removeTypingExpressionsIsIdentity() {
		NullConstant n = new NullConstant(SyntheticLocation.INSTANCE);
		assertSame(n, n.removeTypingExpressions());
	}

	@Test
	public void acceptDispatchesToTheNullConstantOverload() throws Exception {
		NullConstant n = new NullConstant(SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = n.accept(visitor);
		assertSame(n, result);
		assertEquals(java.util.Collections.singletonList(n), visitor.visited);
	}

	@Test
	public void replaceSubstitutesWhenItMatchesTheSourceOtherwiseIdentity() {
		NullConstant n = new NullConstant(SyntheticLocation.INSTANCE);
		it.unive.lisa.symbolic.value.Constant target = new it.unive.lisa.symbolic.value.Constant(
				NullType.INSTANCE, 1, SyntheticLocation.INSTANCE);
		assertSame(target, n.replace(n, target));
		assertFalse(n.replace(target, n) == target);
	}

}
