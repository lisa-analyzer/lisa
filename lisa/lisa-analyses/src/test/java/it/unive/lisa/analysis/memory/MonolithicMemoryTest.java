package it.unive.lisa.analysis.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.memory.Monolith;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.memory.GetAddress;
import it.unive.lisa.symbolic.memory.MemoryAllocation;
import it.unive.lisa.symbolic.memory.MemoryDereference;
import it.unive.lisa.symbolic.memory.StaticAccess;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryLocation;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class MonolithicMemoryTest {

	private final MonolithicMemory heap = new MonolithicMemory();

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
		Pair<Monolith, List<MemoryReplacement>> a = heap.assign(
				Monolith.SINGLETON, x, new Constant(Int32Type.INSTANCE, 1, loc), pp, oracle);
		assertEquals(Monolith.SINGLETON, a.getLeft());
		assertTrue(a.getRight().isEmpty());

		Pair<Monolith, List<MemoryReplacement>> s = heap.semanticsOf(
				Monolith.SINGLETON, new MemoryAllocation(Untyped.INSTANCE, loc), pp, oracle);
		assertEquals(Monolith.SINGLETON, s.getLeft());

		Pair<Monolith, List<MemoryReplacement>> as = heap.assume(
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
		MemoryLocation first = (MemoryLocation) firstAlloc.elements.iterator().next();
		MemoryLocation second = (MemoryLocation) secondAlloc.elements.iterator().next();
		// two allocations at different program points still collapse onto
		// the same monolith identifier: this is exactly what "monolithic"
		// means (all heap locations abstracted into one)
		assertEquals(first, second);
		assertTrue(first.isAllocation());
	}

	@Test
	public void heapDereferenceIsRewrittenAsAnIdentity()
			throws SemanticException {
		MemoryDereference deref = new MemoryDereference(Untyped.INSTANCE, x, loc);
		ExpressionSet input = new ExpressionSet(x);
		assertEquals(input, heap.rewriteMemoryDereference(deref, input, Monolith.SINGLETON, pp, oracle));
	}

	@Test
	public void heapReferenceIsRewrittenToAPointerToTheMonolith()
			throws SemanticException {
		ExpressionSet alloc = heap.rewriteMemoryAllocation(
				new MemoryAllocation(Untyped.INSTANCE, loc), Monolith.SINGLETON, pp, oracle);
		GetAddress ref = new GetAddress(Untyped.INSTANCE, new MemoryAllocation(Untyped.INSTANCE, loc), loc);
		ExpressionSet rewritten = heap.rewriteGetAddress(ref, alloc, Monolith.SINGLETON, pp, oracle);
		assertEquals(1, rewritten.size());
		assertTrue(rewritten.elements.iterator().next() instanceof MemoryPointer);
	}

	@Test
	public void accessChildIsRewrittenToTheMonolith()
			throws SemanticException {
		ExpressionSet receiver = heap.rewriteMemoryAllocation(
				new MemoryAllocation(Untyped.INSTANCE, loc), Monolith.SINGLETON, pp, oracle);
		Variable field = new Variable(Untyped.INSTANCE, "x", loc);
		StaticAccess access = new StaticAccess(Untyped.INSTANCE, x, field, loc);
		ExpressionSet rewritten = heap.rewriteStaticAccess(
				access, receiver, field, Monolith.SINGLETON, pp, oracle);
		assertEquals(1, rewritten.size());
		assertTrue(rewritten.elements.iterator().next() instanceof MemoryLocation);
	}

	@Test
	public void aliasingAndReachabilityAreAlwaysUnknown()
			throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN, heap.alias(Monolith.SINGLETON, x, x, pp, oracle));
		assertEquals(Satisfiability.UNKNOWN, heap.isReachableFrom(Monolith.SINGLETON, x, x, pp, oracle));
	}

}
