package it.unive.lisa.analysis.memory.pointbased;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.StringSet;
import it.unive.lisa.lattices.memory.allocations.AllocationSite;
import it.unive.lisa.lattices.memory.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.memory.allocations.MemoryEnvWithFields;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.memory.MemoryAllocation;
import it.unive.lisa.symbolic.memory.StaticAccess;
import it.unive.lisa.symbolic.value.MemoryLocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import org.junit.jupiter.api.Test;

public class FieldSensitivePointBasedMemoryTest {

	private final FieldSensitivePointBasedMemory heap = new FieldSensitivePointBasedMemory();

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	private final ProgramPoint pp = new TestParameterProvider.FakePP();

	private final SemanticOracle oracle = new TestParameterProvider.FakeOracle();

	@Test
	public void theFirstAllocationAtALocationIsStrong()
			throws SemanticException {
		MemoryAllocation alloc = new MemoryAllocation(Untyped.INSTANCE, loc);
		ExpressionSet rewritten = heap.rewriteMemoryAllocation(alloc, heap.makeLattice(), pp, oracle);
		assertEquals(1, rewritten.size());
		MemoryLocation site = (MemoryLocation) rewritten.elements.iterator().next();
		assertFalse(site.isWeak());
	}

	@Test
	public void reAllocatingTheSameLocationProducesAWeakSite()
			throws SemanticException {
		HeapAllocationSite existing = new HeapAllocationSite(
				Untyped.INSTANCE, loc.getCodeLocation(), false, loc);
		MemoryEnvWithFields state = heap.store(
				heap.makeLattice(), new Variable(Untyped.INSTANCE, "x", loc), existing);

		MemoryAllocation alloc = new MemoryAllocation(Untyped.INSTANCE, loc);
		ExpressionSet rewritten = heap.rewriteMemoryAllocation(alloc, state, pp, oracle);
		assertEquals(1, rewritten.size());
		MemoryLocation site = (MemoryLocation) rewritten.elements.iterator().next();
		// this is what makes the analysis sound in presence of loops or
		// repeated allocations at the same program point: once a location has
		// already been allocated, any further allocation there can no longer
		// be assumed to represent a single, distinct runtime object
		assertTrue(site.isWeak());
	}

	@Test
	public void addFieldAccumulatesFieldsForTheSameSite() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);

		PatriciaTrieMap<AllocationSite, StringSet> mapping = PatriciaTrieMap.empty();
		mapping = heap.addField(site, "f1", mapping);
		mapping = heap.addField(site, "f2", mapping);

		assertEquals(1, mapping.size());
		assertTrue(mapping.get(site).elements().contains("f1"));
		assertTrue(mapping.get(site).elements().contains("f2"));
	}

	@Test
	public void accessingAFieldOnNullProducesTheNullSiteItself()
			throws SemanticException {
		StaticAccess access = new StaticAccess(
				Untyped.INSTANCE,
				new Variable(Untyped.INSTANCE, "x", loc),
				new Variable(Untyped.INSTANCE, "f", loc),
				loc);
		ExpressionSet receiver = new ExpressionSet(
				it.unive.lisa.lattices.memory.allocations.NullAllocationSite.INSTANCE);
		Variable child = new Variable(Untyped.INSTANCE, "f", loc);

		ExpressionSet rewritten = heap.rewriteStaticAccess(access, receiver, child, heap.makeLattice(), pp, oracle);
		assertEquals(1, rewritten.size());
		assertEquals(
				it.unive.lisa.lattices.memory.allocations.NullAllocationSite.INSTANCE,
				rewritten.elements.iterator().next());
	}

}
