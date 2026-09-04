package it.unive.lisa.analysis.combination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.lattices.SingleHeapLattice;
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
 * Tests {@link HeapLatticeProduct}, checking that the replacement lists
 * produced by the two components are combined by concatenation.
 */
public class HeapLatticeProductTest {

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

	private HeapLatticeProduct<SingleHeapLattice, SingleHeapLattice> mk() {
		return new HeapLatticeProduct<>(SingleHeapLattice.SINGLETON, SingleHeapLattice.SINGLETON);
	}

	@Test
	public void testExpandConcatenatesBothComponents()
			throws SemanticException {
		HeapLatticeProduct<SingleHeapLattice, SingleHeapLattice> product = mk();
		HeapReplacement base = new HeapReplacement().withSource(x);

		List<HeapReplacement> expanded = product.expand(base);

		// SingleHeapLattice#expand is the identity, so each of the two
		// components contributes exactly the base replacement
		assertEquals(List.of(base, base), expanded);
	}

	@Test
	public void testPushScopeDelegatesToBothComponents()
			throws SemanticException {
		HeapLatticeProduct<SingleHeapLattice, SingleHeapLattice> product = mk();

		Pair<HeapLatticeProduct<SingleHeapLattice, SingleHeapLattice>, List<HeapReplacement>> pushed = product
				.pushScope(token, fake);

		assertEquals(SingleHeapLattice.SINGLETON, pushed.getLeft().first);
		assertEquals(SingleHeapLattice.SINGLETON, pushed.getLeft().second);
		assertTrue(pushed.getRight().isEmpty());
	}

	@Test
	public void testMkCombinesReplacementsInOrder() {
		HeapReplacement r1 = new HeapReplacement().withSource(x);
		HeapReplacement r2 = new HeapReplacement().withSource(x).withTarget(x);

		Pair<HeapLatticeProduct<SingleHeapLattice, SingleHeapLattice>, List<HeapReplacement>> combined = mk().mk(
				Pair.of(SingleHeapLattice.SINGLETON, List.of(r1)),
				Pair.of(SingleHeapLattice.BOTTOM, List.of(r2)));

		assertEquals(SingleHeapLattice.SINGLETON, combined.getLeft().first);
		assertEquals(SingleHeapLattice.BOTTOM, combined.getLeft().second);
		assertEquals(List.of(r1, r2), combined.getRight());
	}

}
