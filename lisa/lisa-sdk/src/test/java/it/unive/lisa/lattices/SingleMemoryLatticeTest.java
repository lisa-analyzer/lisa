package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SingleMemoryLatticeTest {

	@Test
	public void topIsTheSingletonAndBottomIsDistinct() {
		assertSame(SingleMemoryLattice.SINGLETON, SingleMemoryLattice.SINGLETON.top());
		assertSame(SingleMemoryLattice.BOTTOM, SingleMemoryLattice.SINGLETON.bottom());
		assertTrue(SingleMemoryLattice.SINGLETON.isTop());
		assertTrue(SingleMemoryLattice.BOTTOM.isBottom());
	}

	@Test
	public void lubAndGlbFollowTheTwoElementChain() throws SemanticException {
		assertSame(SingleMemoryLattice.BOTTOM, SingleMemoryLattice.BOTTOM.lub(SingleMemoryLattice.BOTTOM));
		assertSame(SingleMemoryLattice.SINGLETON, SingleMemoryLattice.BOTTOM.lub(SingleMemoryLattice.SINGLETON));
		assertSame(SingleMemoryLattice.SINGLETON, SingleMemoryLattice.SINGLETON.glb(SingleMemoryLattice.SINGLETON));
		assertSame(SingleMemoryLattice.BOTTOM, SingleMemoryLattice.SINGLETON.glb(SingleMemoryLattice.BOTTOM));
	}

	@Test
	public void scopeAndForgetOperationsNeverProduceReplacements() throws SemanticException {
		assertTrue(SingleMemoryLattice.SINGLETON.pushScope(null, null).getRight().isEmpty());
		assertTrue(SingleMemoryLattice.SINGLETON.popScope(null, null).getRight().isEmpty());
		assertTrue(SingleMemoryLattice.SINGLETON.forgetIdentifier(null, null).getRight().isEmpty());
		assertTrue(SingleMemoryLattice.SINGLETON.forgetIdentifiers(null, null).getRight().isEmpty());
		assertTrue(SingleMemoryLattice.SINGLETON.forgetIdentifiersIf(null, null).getRight().isEmpty());
	}

	@Test
	public void expandIsIdentityAndReachableOnlyFromKeepsEveryIdentifier() throws SemanticException {
		MemoryReplacement base = new MemoryReplacement();
		assertTrue(SingleMemoryLattice.SINGLETON.expand(base).equals(List.of(base)));

		Collection<Identifier> ids = List.of();
		GenericMapLattice<Identifier,
				SingleMemoryLattice> state = new GenericMapLattice<>(SingleMemoryLattice.SINGLETON);
		assertSame(ids, SingleMemoryLattice.SINGLETON.reachableOnlyFrom(state, ids));
	}

}
