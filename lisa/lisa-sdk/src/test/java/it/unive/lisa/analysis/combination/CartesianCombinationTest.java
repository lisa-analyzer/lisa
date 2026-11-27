package it.unive.lisa.analysis.combination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.SingleTypeLattice;
import it.unive.lisa.lattices.SingleValueLattice;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CartesianCombination}, exercised through {@link LatticeProduct}
 * since {@link CartesianCombination} is abstract. No reduction is performed by
 * {@link LatticeProduct}, so every operation must be the pointwise application
 * of the corresponding operation on the two components.
 */
public class CartesianCombinationTest {

	private LatticeProduct<SingleValueLattice, SingleTypeLattice> mk(
			SingleValueLattice first,
			SingleTypeLattice second) {
		return new LatticeProduct<>(first, second);
	}

	@Test
	public void testTopIsTopOnBothComponents() {
		LatticeProduct<SingleValueLattice, SingleTypeLattice> top = mk(SingleValueLattice.SINGLETON,
				SingleTypeLattice.SINGLETON).top();

		assertTrue(top.isTop());
		assertEquals(SingleValueLattice.SINGLETON, top.first);
		assertEquals(SingleTypeLattice.SINGLETON, top.second);
	}

	@Test
	public void testBottomIsBottomOnBothComponents() {
		LatticeProduct<SingleValueLattice, SingleTypeLattice> bottom = mk(SingleValueLattice.SINGLETON,
				SingleTypeLattice.SINGLETON).bottom();

		assertTrue(bottom.isBottom());
		assertEquals(SingleValueLattice.BOTTOM, bottom.first);
		assertEquals(SingleTypeLattice.BOTTOM, bottom.second);
	}

	@Test
	public void testIsTopRequiresBothComponentsToBeTop() {
		LatticeProduct<SingleValueLattice, SingleTypeLattice> partial = mk(SingleValueLattice.SINGLETON,
				SingleTypeLattice.BOTTOM);

		assertFalse(partial.isTop());
		assertFalse(partial.isBottom());
	}

	@Test
	public void testLubIsPointwise()
			throws SemanticException {
		LatticeProduct<SingleValueLattice, SingleTypeLattice> a = mk(SingleValueLattice.BOTTOM,
				SingleTypeLattice.SINGLETON);
		LatticeProduct<SingleValueLattice, SingleTypeLattice> b = mk(SingleValueLattice.SINGLETON,
				SingleTypeLattice.BOTTOM);

		LatticeProduct<SingleValueLattice, SingleTypeLattice> lub = a.lub(b);

		assertEquals(SingleValueLattice.SINGLETON, lub.first);
		assertEquals(SingleTypeLattice.SINGLETON, lub.second);
	}

	@Test
	public void testGlbIsPointwise()
			throws SemanticException {
		LatticeProduct<SingleValueLattice, SingleTypeLattice> a = mk(SingleValueLattice.SINGLETON,
				SingleTypeLattice.SINGLETON);
		LatticeProduct<SingleValueLattice, SingleTypeLattice> b = mk(SingleValueLattice.BOTTOM,
				SingleTypeLattice.SINGLETON);

		LatticeProduct<SingleValueLattice, SingleTypeLattice> glb = a.glb(b);

		assertEquals(SingleValueLattice.BOTTOM, glb.first);
		assertEquals(SingleTypeLattice.SINGLETON, glb.second);
	}

	@Test
	public void testLessOrEqualRequiresBothComponents()
			throws SemanticException {
		LatticeProduct<SingleValueLattice, SingleTypeLattice> bottomTop = mk(SingleValueLattice.BOTTOM,
				SingleTypeLattice.SINGLETON);
		LatticeProduct<SingleValueLattice, SingleTypeLattice> topTop = mk(SingleValueLattice.SINGLETON,
				SingleTypeLattice.SINGLETON);

		assertTrue(bottomTop.lessOrEqual(topTop));
		assertFalse(topTop.lessOrEqual(bottomTop));
	}

}
