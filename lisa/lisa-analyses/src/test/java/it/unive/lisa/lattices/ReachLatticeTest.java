package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ReachLattice.ReachabilityStatus;
import org.junit.jupiter.api.Test;

public class ReachLatticeTest {

	@Test
	public void defaultConstructorIsPossiblyReachable() {
		assertEquals(ReachabilityStatus.POSSIBLY_REACHABLE, new ReachLattice().lattice);
		assertTrue(new ReachLattice().isTop());
	}

	@Test
	public void topIsPossiblyReachable() {
		assertEquals(ReachabilityStatus.POSSIBLY_REACHABLE, ReachabilityStatus.POSSIBLY_REACHABLE.top());
		assertTrue(new ReachLattice(ReachabilityStatus.UNREACHABLE, null).top().isTop());
	}

	@Test
	public void bottomIsUnreachable() {
		assertEquals(ReachabilityStatus.UNREACHABLE, ReachabilityStatus.UNREACHABLE.bottom());
		assertTrue(new ReachLattice(ReachabilityStatus.POSSIBLY_REACHABLE, null).bottom().isBottom());
	}

	@Test
	public void setToReachableChangesTheStatus() {
		ReachLattice r = new ReachLattice(ReachabilityStatus.UNREACHABLE, null);
		assertEquals(ReachabilityStatus.REACHABLE, r.setToReachable().lattice);
	}

	@Test
	public void setToUnreachableChangesTheStatus() {
		ReachLattice r = new ReachLattice();
		assertEquals(ReachabilityStatus.UNREACHABLE, r.setToUnreachable().lattice);
	}

	@Test
	public void setToPossiblyReachableChangesTheStatus() {
		ReachLattice r = new ReachLattice(ReachabilityStatus.UNREACHABLE, null);
		assertEquals(ReachabilityStatus.POSSIBLY_REACHABLE, r.setToPossiblyReachable().lattice);
	}

	@Test
	public void settersAreIdempotentWhenAlreadyInThatStatus() {
		ReachLattice reachable = new ReachLattice(ReachabilityStatus.REACHABLE, null);
		assertTrue(reachable == reachable.setToReachable());
	}

	@Test
	public void noneOfTheStatusesKnowIdentifiers() {
		assertFalse(new ReachLattice().knowsIdentifier(null));
	}

	@Test
	public void forgetOperationsAreNoOps()
			throws SemanticException {
		ReachLattice r = new ReachLattice();
		assertTrue(r == r.forgetIdentifier(null, null));
		assertTrue(r == r.forgetIdentifiers(null, null));
		assertTrue(r == r.forgetIdentifiersIf(null, null));
	}

	@Test
	public void scopePushAndPopAreNoOps()
			throws SemanticException {
		ReachLattice r = new ReachLattice();
		assertTrue(r == r.pushScope(null, null));
		assertTrue(r == r.popScope(null, null));
	}

}
