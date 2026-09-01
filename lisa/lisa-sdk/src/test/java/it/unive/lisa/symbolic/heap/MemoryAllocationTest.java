package it.unive.lisa.symbolic.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.symbolic.RecordingVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class MemoryAllocationTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	@Test
	public void defaultConstructorIsNotAStackAllocationAndHasNoAnnotations() {
		MemoryAllocation m = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertFalse(m.isStackAllocation());
		assertTrue(m.getAnnotationList().isEmpty());
	}

	@Test
	public void isStackAllocationReflectsTheConstructorArgument() {
		MemoryAllocation stack = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, true);
		MemoryAllocation heap = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, false);
		assertTrue(stack.isStackAllocation());
		assertFalse(heap.isStackAllocation());
	}

	@Test
	public void annotationsConstructorStoresAndExposesThem() {
		Annotations anns = new Annotations();
		anns.addAnnotation(new Annotation("ann1"));
		MemoryAllocation m = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, anns);
		assertSame(anns, m.getAnnotations());
		assertEquals(1, m.getAnnotationList().size());
	}

	@Test
	public void toStringPrefixesWithNewUnlessStackAllocated() {
		MemoryAllocation heap = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, false);
		MemoryAllocation stack = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, true);
		assertEquals("new " + Untyped.INSTANCE, heap.toString());
		assertEquals(Untyped.INSTANCE.toString(), stack.toString());
	}

	@Test
	public void equalsComparesAnnotationsAndStackAllocationFlag() {
		MemoryAllocation a = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, true);
		MemoryAllocation b = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, true);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		MemoryAllocation different = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, false);
		assertFalse(a.equals(different));
	}

	@Test
	public void mightNeedRewritingIsAlwaysTrue() {
		assertTrue(new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE).mightNeedRewriting());
	}

	@Test
	public void acceptDispatchesToTheMemoryAllocationOverload() throws Exception {
		MemoryAllocation m = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		RecordingVisitor visitor = new RecordingVisitor();
		SymbolicExpression result = m.accept(visitor);
		assertSame(m, result);
		assertEquals(java.util.Collections.singletonList(m), visitor.visited);
	}

	@Test
	public void pushPopScopeReplaceAndRemoveTypingExpressionsAreAllNoOps() throws Exception {
		MemoryAllocation m = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertSame(m, m.pushScope(TOKEN, null));
		assertSame(m, m.popScope(TOKEN, null));
		assertSame(m, m.removeTypingExpressions());
		MemoryAllocation target = new MemoryAllocation(Untyped.INSTANCE, SyntheticLocation.INSTANCE, true);
		assertSame(target, m.replace(m, target));
	}

}
