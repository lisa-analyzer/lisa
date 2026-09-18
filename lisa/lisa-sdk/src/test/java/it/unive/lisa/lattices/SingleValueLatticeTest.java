package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class SingleValueLatticeTest {

	@Test
	public void topIsTheSingletonAndBottomIsDistinct() {
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.top());
		assertSame(SingleValueLattice.BOTTOM, SingleValueLattice.SINGLETON.bottom());
		assertTrue(SingleValueLattice.SINGLETON.isTop());
		assertTrue(SingleValueLattice.BOTTOM.isBottom());
		assertFalse(SingleValueLattice.BOTTOM.isTop());
		assertFalse(SingleValueLattice.SINGLETON.isBottom());
	}

	@Test
	public void lessOrEqualFollowsTheTwoElementChain() throws SemanticException {
		assertTrue(SingleValueLattice.BOTTOM.lessOrEqual(SingleValueLattice.SINGLETON));
		assertTrue(SingleValueLattice.BOTTOM.lessOrEqual(SingleValueLattice.BOTTOM));
		assertTrue(SingleValueLattice.SINGLETON.lessOrEqual(SingleValueLattice.SINGLETON));
		assertFalse(SingleValueLattice.SINGLETON.lessOrEqual(SingleValueLattice.BOTTOM));
	}

	@Test
	public void lubIsBottomOnlyWhenBothAreBottom() throws SemanticException {
		assertSame(SingleValueLattice.BOTTOM, SingleValueLattice.BOTTOM.lub(SingleValueLattice.BOTTOM));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.BOTTOM.lub(SingleValueLattice.SINGLETON));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.lub(SingleValueLattice.BOTTOM));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.lub(SingleValueLattice.SINGLETON));
	}

	@Test
	public void glbIsSingletonOnlyWhenBothAreSingleton() throws SemanticException {
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.glb(SingleValueLattice.SINGLETON));
		assertSame(SingleValueLattice.BOTTOM, SingleValueLattice.SINGLETON.glb(SingleValueLattice.BOTTOM));
		assertSame(SingleValueLattice.BOTTOM, SingleValueLattice.BOTTOM.glb(SingleValueLattice.SINGLETON));
	}

	@Test
	public void pushPopScopeAndAllForgetOperationsAreNoOps() throws SemanticException {
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.pushScope(null, null));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.popScope(null, null));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.forgetIdentifier(null, null));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.forgetIdentifiers(null, null));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.forgetIdentifiersIf(null, null));
		assertSame(SingleValueLattice.SINGLETON, SingleValueLattice.SINGLETON.store(null, null));
	}

	@Test
	public void knowsIdentifierIsAlwaysFalse() {
		assertFalse(SingleValueLattice.SINGLETON.knowsIdentifier(null));
	}

}
