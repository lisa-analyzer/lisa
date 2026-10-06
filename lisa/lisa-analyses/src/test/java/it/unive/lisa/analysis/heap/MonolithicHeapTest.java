package it.unive.lisa.analysis.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class MonolithicHeapTest {

	private final MonolithicHeap heap = new MonolithicHeap();

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	private final ProgramPoint pp = new TestParameterProvider.FakePP();

	private final SemanticOracle oracle = new TestParameterProvider.FakeOracle();

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", loc);

	@Test
	public void makeLatticeIsTheSingleton() {
		assertEquals(Monolith.SINGLETON, heap.makeLattice());
	}

	@Test
	public void assignSemanticsOfAndAssumeDoNotAffectTheHeap()
			throws SemanticException {
		Pair<Monolith, List<HeapReplacement>> a = heap.assign(
				Monolith.SINGLETON, x, new Constant(Int32Type.INSTANCE, 1, loc), pp, oracle);
		assertEquals(Monolith.SINGLETON, a.getLeft());
		assertTrue(a.getRight().isEmpty());

		Pair<Monolith, List<HeapReplacement>> s = heap.semanticsOf(
				Monolith.SINGLETON, new MemoryAllocation(Untyped.INSTANCE, loc), pp, oracle);
		assertEquals(Monolith.SINGLETON, s.getLeft());

		Pair<Monolith, List<HeapReplacement>> as = heap.assume(
				Monolith.SINGLETON, new Constant(Int32Type.INSTANCE, 1, loc), pp, pp, oracle);
		assertEquals(Monolith.SINGLETON, as.getLeft());
	}

	@Test
	public void everyAllocationIsRewrittenToTheSameMonolithLocation()
			throws SemanticException {
		ExpressionSet firstAlloc = heap.rewriteMemoryAllocation(
				new MemoryAllocation(Untyped.INSTANCE, loc), Monolith.SINGLETON, pp, oracle);
		ExpressionSet secondAlloc = heap.rewriteMemoryAllocation(
				new MemoryAllocation(Untyped.INSTANCE, new SourceCodeLocation("fake", 2, 2)),
				Monolith.SINGLETON, pp, oracle);

		assertEquals(1, firstAlloc.size());
		assertEquals(1, secondAlloc.size());
		HeapLocation first = (HeapLocation) firstAlloc.elements.iterator().next();
		HeapLocation second = (HeapLocation) secondAlloc.elements.iterator().next();
		// two allocations at different program points still collapse onto
		// the same monolith identifier: this is exactly what "monolithic"
		// means (all heap locations abstracted into one)
		assertEquals(first, second);
		assertTrue(first.isAllocation());
	}

	@Test
	public void heapDereferenceIsRewrittenAsAnIdentity()
			throws SemanticException {
		HeapDereference deref = new HeapDereference(Untyped.INSTANCE, x, loc);
		ExpressionSet input = new ExpressionSet(x);
		assertEquals(input, heap.rewriteHeapDereference(deref, input, Monolith.SINGLETON, pp, oracle));
	}

	@Test
	public void heapReferenceIsRewrittenToAPointerToTheMonolith()
			throws SemanticException {
		ExpressionSet alloc = heap.rewriteMemoryAllocation(
				new MemoryAllocation(Untyped.INSTANCE, loc), Monolith.SINGLETON, pp, oracle);
		HeapReference ref = new HeapReference(Untyped.INSTANCE, new MemoryAllocation(Untyped.INSTANCE, loc), loc);
		ExpressionSet rewritten = heap.rewriteHeapReference(ref, alloc, Monolith.SINGLETON, pp, oracle);
		assertEquals(1, rewritten.size());
		assertTrue(rewritten.elements.iterator().next() instanceof MemoryPointer);
	}

	@Test
	public void accessChildIsRewrittenToTheMonolith()
			throws SemanticException {
		ExpressionSet receiver = heap.rewriteMemoryAllocation(
				new MemoryAllocation(Untyped.INSTANCE, loc), Monolith.SINGLETON, pp, oracle);
		AccessChild access = new AccessChild(Untyped.INSTANCE, x, x, loc);
		ExpressionSet rewritten = heap.rewriteAccessChild(
				access, receiver, new ExpressionSet(x), Monolith.SINGLETON, pp, oracle);
		assertEquals(1, rewritten.size());
		assertTrue(rewritten.elements.iterator().next() instanceof HeapLocation);
	}

	@Test
	public void aliasingAndReachabilityAreAlwaysUnknown()
			throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN, heap.alias(Monolith.SINGLETON, x, x, pp, oracle));
		assertEquals(Satisfiability.UNKNOWN, heap.isReachableFrom(Monolith.SINGLETON, x, x, pp, oracle));
	}

}
