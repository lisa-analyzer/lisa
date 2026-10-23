package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.ReachLattice.ReachabilityStatus;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.util.numeric.IntInterval;
import org.junit.jupiter.api.Test;

public class ReachabilityProductTest {

	private SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>> state() {
		return new SimpleAbstractState<>(
				Monolith.SINGLETON,
				new ValueEnvironment<>(IntInterval.TOP),
				new TypeEnvironment<>(new TypeSet()));
	}

	@Test
	public void mkReducesToUnreachableWhenTheStateIsBottom()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> p = new ReachabilityProduct<>(new ReachLattice(), state());
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> reduced = p
						.mk(new ReachLattice(), state().bottom());
		assertEquals(ReachabilityStatus.UNREACHABLE, reduced.first.lattice);
	}

	@Test
	public void mkDoesNotAlterReachabilityWhenTheStateIsNotBottom()
			throws SemanticException {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> p = new ReachabilityProduct<>(new ReachLattice(), state());
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> reduced = p
						.mk(new ReachLattice(ReachabilityStatus.REACHABLE, null), state());
		assertEquals(ReachabilityStatus.REACHABLE, reduced.first.lattice);
	}

	@Test
	public void setToReachableChangesOnlyTheReachabilityComponent() {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> p = new ReachabilityProduct<>(
						new ReachLattice(ReachabilityStatus.UNREACHABLE, null), state());
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> reached = p
						.setToReachable();
		assertEquals(ReachabilityStatus.REACHABLE, reached.first.lattice);
		assertEquals(p.second, reached.second);
	}

	@Test
	public void setToUnreachableChangesOnlyTheReachabilityComponent() {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> p = new ReachabilityProduct<>(new ReachLattice(), state());
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> unreached = p
						.setToUnreachable();
		assertEquals(ReachabilityStatus.UNREACHABLE, unreached.first.lattice);
		assertEquals(p.second, unreached.second);
	}

	@Test
	public void setToPossiblyReachableChangesOnlyTheReachabilityComponent() {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> p = new ReachabilityProduct<>(
						new ReachLattice(ReachabilityStatus.UNREACHABLE, null), state());
		ReachabilityProduct<
				SimpleAbstractState<Monolith, ValueEnvironment<IntInterval>, TypeEnvironment<TypeSet>>> possibly = p
						.setToPossiblyReachable();
		assertEquals(ReachabilityStatus.POSSIBLY_REACHABLE, possibly.first.lattice);
		assertEquals(p.second, possibly.second);
	}

	@Test
	public void setterIsIdempotentWhenAlreadyInThatStatus() {
		ReachabilityProduct<SimpleAbstractState<Monolith,
				ValueEnvironment<IntInterval>,
				TypeEnvironment<TypeSet>>> p = new ReachabilityProduct<>(
						new ReachLattice(ReachabilityStatus.REACHABLE, null), state());
		assertTrue(p == p.setToReachable());
	}

}
