package it.unive.lisa.analysis.heap.pointbased;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.heap.allocations.HeapEnvWithFields;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import org.junit.jupiter.api.Test;

public class FieldSensitivePointBasedHeapTest {

	private final FieldSensitivePointBasedHeap heap = new FieldSensitivePointBasedHeap();

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	private final ProgramPoint pp = new TestParameterProvider.FakePP();

	private final SemanticOracle oracle = new TestParameterProvider.FakeOracle();

	@Test
	public void theFirstAllocationAtALocationIsStrong()
			throws SemanticException {
		MemoryAllocation alloc = new MemoryAllocation(Untyped.INSTANCE, loc);
		ExpressionSet rewritten = heap.rewriteMemoryAllocation(alloc, heap.makeLattice(), pp, oracle);
		assertEquals(1, rewritten.size());
		HeapLocation site = (HeapLocation) rewritten.elements.iterator().next();
		assertFalse(site.isWeak());
	}

	@Test
	public void reAllocatingTheSameLocationProducesAWeakSite()
			throws SemanticException {
		HeapAllocationSite existing = new HeapAllocationSite(
				Untyped.INSTANCE, loc.getCodeLocation(), false, loc);
		HeapEnvWithFields state = heap.store(
				heap.makeLattice(), new Variable(Untyped.INSTANCE, "x", loc), existing);

		MemoryAllocation alloc = new MemoryAllocation(Untyped.INSTANCE, loc);
		ExpressionSet rewritten = heap.rewriteMemoryAllocation(alloc, state, pp, oracle);
		assertEquals(1, rewritten.size());
		HeapLocation site = (HeapLocation) rewritten.elements.iterator().next();
		// this is what makes the analysis sound in presence of loops or
		// repeated allocations at the same program point: once a location has
		// already been allocated, any further allocation there can no longer
		// be assumed to represent a single, distinct runtime object
		assertTrue(site.isWeak());
	}

	@Test
	public void addFieldAccumulatesFieldsForTheSameSite() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		Variable f1 = new Variable(Untyped.INSTANCE, "f1", loc);
		Variable f2 = new Variable(Untyped.INSTANCE, "f2", loc);

		PatriciaTrieMap<AllocationSite, ExpressionSet> mapping = PatriciaTrieMap.empty();
		mapping = heap.addField(site, f1, mapping);
		mapping = heap.addField(site, f2, mapping);

		assertEquals(1, mapping.size());
		assertTrue(mapping.get(site).elements.contains(f1));
		assertTrue(mapping.get(site).elements.contains(f2));
	}

	@Test
	public void accessingAFieldOnNullProducesTheNullSiteItself()
			throws SemanticException {
		AccessChild access = new AccessChild(
				Untyped.INSTANCE,
				new Variable(Untyped.INSTANCE, "x", loc),
				new Variable(Untyped.INSTANCE, "f", loc),
				loc);
		ExpressionSet receiver = new ExpressionSet(
				it.unive.lisa.lattices.heap.allocations.NullAllocationSite.INSTANCE);
		ExpressionSet child = new ExpressionSet(new Variable(Untyped.INSTANCE, "f", loc));

		ExpressionSet rewritten = heap.rewriteAccessChild(access, receiver, child, heap.makeLattice(), pp, oracle);
		assertEquals(1, rewritten.size());
		assertEquals(
				it.unive.lisa.lattices.heap.allocations.NullAllocationSite.INSTANCE,
				rewritten.elements.iterator().next());
	}

	@Test
	public void strongUpdatesAreDecidedPerContext()
			throws SemanticException {
		ScopeId<?> ctx1 = ContextOracles.context(1);
		ScopeId<?> ctx2 = ContextOracles.context(2);
		FieldSensitivePointBasedHeap sensitive = new FieldSensitivePointBasedHeap(true);
		HeapAllocationSite existing = new HeapAllocationSite(
				Untyped.INSTANCE, loc.getCodeLocation(), ctx1, false, loc);
		HeapEnvWithFields state = sensitive.store(
				sensitive.makeLattice(), new Variable(Untyped.INSTANCE, "x", loc), existing);
		MemoryAllocation alloc = new MemoryAllocation(Untyped.INSTANCE, loc);

		// same location, different context: this is a fresh object
		HeapLocation other = (HeapLocation) sensitive
				.rewriteMemoryAllocation(alloc, state, pp, ContextOracles.oracle(ctx2)).elements.iterator().next();
		assertFalse(other.isWeak());

		// same location and context: the site must become weak
		HeapLocation same = (HeapLocation) sensitive
				.rewriteMemoryAllocation(alloc, state, pp, ContextOracles.oracle(ctx1)).elements.iterator().next();
		assertTrue(same.isWeak());

		// a context-insensitive heap only considers the location
		HeapLocation insensitive = (HeapLocation) heap
				.rewriteMemoryAllocation(alloc, heap.store(heap.makeLattice(), new Variable(Untyped.INSTANCE, "x", loc),
						new HeapAllocationSite(Untyped.INSTANCE, loc.getCodeLocation(), false, loc)),
						pp, ContextOracles.oracle(ctx2)).elements.iterator().next();
		assertTrue(insensitive.isWeak());
	}

	@Test
	public void fieldsInheritTheContextOfTheReceiver()
			throws SemanticException {
		ScopeId<?> ctx1 = ContextOracles.context(1);
		FieldSensitivePointBasedHeap sensitive = new FieldSensitivePointBasedHeap(true);
		HeapAllocationSite receiver = new HeapAllocationSite(Untyped.INSTANCE, "l", ctx1, false, loc);
		Variable field = new Variable(Untyped.INSTANCE, "f", loc);
		AccessChild access = new AccessChild(Untyped.INSTANCE, new Variable(Untyped.INSTANCE, "x", loc), field, loc);

		ExpressionSet rewritten = sensitive.rewriteAccessChild(
				access,
				new ExpressionSet(receiver),
				new ExpressionSet(field),
				sensitive.makeLattice(),
				pp,
				ContextOracles.oracle(ContextOracles.context(2)));
		assertEquals(1, rewritten.size());
		AllocationSite site = (AllocationSite) rewritten.elements.iterator().next();
		assertEquals(ctx1, site.getContext());
		assertEquals("pp@l<" + ctx1.getHash() + ">[f]", site.getName());
	}

}
