package it.unive.lisa.analysis.combination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.lattices.SingleMemoryLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MemoryLatticeProduct}, checking that the replacement lists
 * produced by the two components are combined by concatenation.
 */
public class MemoryLatticeProductTest {

	private static final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private static final ScopeToken token = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private MemoryLatticeProduct<SingleMemoryLattice, SingleMemoryLattice> mk() {
		return new MemoryLatticeProduct<>(SingleMemoryLattice.SINGLETON, SingleMemoryLattice.SINGLETON);
	}

	@Test
	public void testExpandConcatenatesBothComponents()
			throws SemanticException {
		MemoryLatticeProduct<SingleMemoryLattice, SingleMemoryLattice> product = mk();
		MemoryReplacement base = new MemoryReplacement().withSource(x);

		List<MemoryReplacement> expanded = product.expand(base);

		// SingleMemoryLattice#expand is the identity, so each of the two
		// components contributes exactly the base replacement
		assertEquals(List.of(base, base), expanded);
	}

	@Test
	public void testPushScopeDelegatesToBothComponents()
			throws SemanticException {
		MemoryLatticeProduct<SingleMemoryLattice, SingleMemoryLattice> product = mk();

		Pair<MemoryLatticeProduct<SingleMemoryLattice, SingleMemoryLattice>, List<MemoryReplacement>> pushed = product
				.pushScope(token, fake);

		assertEquals(SingleMemoryLattice.SINGLETON, pushed.getLeft().first);
		assertEquals(SingleMemoryLattice.SINGLETON, pushed.getLeft().second);
		assertTrue(pushed.getRight().isEmpty());
	}

	@Test
	public void testMkCombinesReplacementsInOrder() {
		MemoryReplacement r1 = new MemoryReplacement().withSource(x);
		MemoryReplacement r2 = new MemoryReplacement().withSource(x).withTarget(x);

		Pair<MemoryLatticeProduct<SingleMemoryLattice, SingleMemoryLattice>,
				List<MemoryReplacement>> combined = mk().mk(
						Pair.of(SingleMemoryLattice.SINGLETON, List.of(r1)),
						Pair.of(SingleMemoryLattice.BOTTOM, List.of(r2)));

		assertEquals(SingleMemoryLattice.SINGLETON, combined.getLeft().first);
		assertEquals(SingleMemoryLattice.BOTTOM, combined.getLeft().second);
		assertEquals(List.of(r1, r2), combined.getRight());
	}

}
