package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SingleHeapLatticeTest {

	@Test
	public void topIsTheSingletonAndBottomIsDistinct() {
		assertSame(SingleHeapLattice.SINGLETON, SingleHeapLattice.SINGLETON.top());
		assertSame(SingleHeapLattice.BOTTOM, SingleHeapLattice.SINGLETON.bottom());
		assertTrue(SingleHeapLattice.SINGLETON.isTop());
		assertTrue(SingleHeapLattice.BOTTOM.isBottom());
	}

	@Test
	public void lubAndGlbFollowTheTwoElementChain() throws SemanticException {
		assertSame(SingleHeapLattice.BOTTOM, SingleHeapLattice.BOTTOM.lub(SingleHeapLattice.BOTTOM));
		assertSame(SingleHeapLattice.SINGLETON, SingleHeapLattice.BOTTOM.lub(SingleHeapLattice.SINGLETON));
		assertSame(SingleHeapLattice.SINGLETON, SingleHeapLattice.SINGLETON.glb(SingleHeapLattice.SINGLETON));
		assertSame(SingleHeapLattice.BOTTOM, SingleHeapLattice.SINGLETON.glb(SingleHeapLattice.BOTTOM));
	}

	@Test
	public void scopeAndForgetOperationsNeverProduceReplacements() throws SemanticException {
		assertTrue(SingleHeapLattice.SINGLETON.pushScope(null, null).getRight().isEmpty());
		assertTrue(SingleHeapLattice.SINGLETON.popScope(null, null).getRight().isEmpty());
		assertTrue(SingleHeapLattice.SINGLETON.forgetIdentifier(null, null).getRight().isEmpty());
		assertTrue(SingleHeapLattice.SINGLETON.forgetIdentifiers(null, null).getRight().isEmpty());
		assertTrue(SingleHeapLattice.SINGLETON.forgetIdentifiersIf(null, null).getRight().isEmpty());
	}

	@Test
	public void expandIsIdentityAndReachableOnlyFromKeepsEveryIdentifier() throws SemanticException {
		HeapReplacement base = new HeapReplacement();
		assertTrue(SingleHeapLattice.SINGLETON.expand(base).equals(List.of(base)));

		Collection<Identifier> ids = List.of();
		GenericMapLattice<Identifier, SingleHeapLattice> state = new GenericMapLattice<>(SingleHeapLattice.SINGLETON);
		assertSame(ids, SingleHeapLattice.SINGLETON.reachableOnlyFrom(state, ids));
	}

}
