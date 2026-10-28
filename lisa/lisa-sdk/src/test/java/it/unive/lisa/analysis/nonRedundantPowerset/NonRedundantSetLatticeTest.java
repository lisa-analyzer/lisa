package it.unive.lisa.analysis.nonRedundantPowerset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the lattice operations defined in {@link NonRedundantSetLattice},
 * instantiated over {@link IntInterval}s (mirroring
 * {@code it.unive.lisa.lattices.numeric.NonRedundantIntervalSet} in
 * lisa-analyses, but kept self-contained here since that class is outside the
 * lisa-sdk module).
 */
public class NonRedundantSetLatticeTest {

	/**
	 * A minimal, self-contained non-redundant powerset of {@link IntInterval}s.
	 */
	private static final class TestPowerset
			extends
			NonRedundantSetLattice<TestPowerset, IntInterval> {

		private TestPowerset(
				Set<IntInterval> elements) {
			super(elements, IntInterval.TOP);
		}

		@Override
		public TestPowerset mk(
				Set<IntInterval> set) {
			return new TestPowerset(set);
		}
	}

	private static TestPowerset of(
			IntInterval... intervals) {
		return new TestPowerset(Set.of(intervals));
	}

	@Test
	public void testBottomIsTheEmptySet() {
		TestPowerset bottom = of();
		assertTrue(bottom.isBottom());
		assertEquals(Set.of(), bottom.elements);
	}

	@Test
	public void testTopIsASingletonContainingTheUnderlyingTop() {
		TestPowerset top = of().top();
		assertTrue(top.isTop());
		assertEquals(Set.of(IntInterval.TOP), top.elements);
	}

	@Test
	public void testLessOrEqualAuxHoldsWhenEveryLeftElementHasACoveringRightElement()
			throws SemanticException {
		TestPowerset small = of(new IntInterval(0, 2), new IntInterval(3, 5));
		TestPowerset big = of(new IntInterval(0, 5));
		assertTrue(small.lessOrEqualAux(big));
		assertFalse(big.lessOrEqualAux(small));
	}

	@Test
	public void testRemoveRedundancyDropsElementsSubsumedByAnother()
			throws SemanticException {
		TestPowerset withRedundancy = of(new IntInterval(0, 5), new IntInterval(1, 2));
		TestPowerset reduced = withRedundancy.removeRedundancy();
		assertEquals(Set.of(new IntInterval(0, 5)), reduced.elements);
	}

	@Test
	public void testRemoveRedundancyDropsBottomElements()
			throws SemanticException {
		TestPowerset withBottom = of(new IntInterval(0, 5), IntInterval.BOTTOM);
		TestPowerset reduced = withBottom.removeRedundancy();
		assertEquals(Set.of(new IntInterval(0, 5)), reduced.elements);
	}

	@Test
	public void testRemoveOverlappingMergesIntervalsWithNonBottomGlb()
			throws SemanticException {
		TestPowerset overlapping = of(new IntInterval(0, 2), new IntInterval(1, 3));
		TestPowerset merged = overlapping.removeOverlapping();
		assertEquals(Set.of(new IntInterval(0, 3)), merged.elements);
	}

	@Test
	public void testRemoveOverlappingKeepsDisjointIntervalsSeparate()
			throws SemanticException {
		TestPowerset disjoint = of(new IntInterval(0, 2), new IntInterval(5, 7));
		TestPowerset result = disjoint.removeOverlapping();
		assertEquals(Set.of(new IntInterval(0, 2), new IntInterval(5, 7)), result.elements);
	}

	@Test
	public void testLubAuxUnionsAndNormalizesTheResult()
			throws SemanticException {
		TestPowerset s1 = of(new IntInterval(0, 2));
		TestPowerset s2 = of(new IntInterval(1, 3));
		// the two intervals overlap, so the lub must merge them into one
		TestPowerset lub = s1.lubAux(s2);
		assertEquals(Set.of(new IntInterval(0, 3)), lub.elements);
	}

	@Test
	public void testGlbAuxIntersectsPairwiseAndNormalizes()
			throws SemanticException {
		TestPowerset s1 = of(new IntInterval(0, 5));
		TestPowerset s2 = of(new IntInterval(3, 10));
		TestPowerset glb = s1.glbAux(s2);
		assertEquals(Set.of(new IntInterval(3, 5)), glb.elements);
	}

	@Test
	public void testLessOrEqualEgliMilnerRequiresEveryRightElementToBeCoveredFromBelow()
			throws SemanticException {
		// small <=_S big holds (every element of small has a covering element
		// in big), but big has an extra element ([5,7]) that is not covered
		// from below by anything in small, so <=_EM must fail
		TestPowerset small = of(new IntInterval(0, 2));
		TestPowerset big = of(new IntInterval(0, 2), new IntInterval(5, 7));
		assertTrue(small.lessOrEqual(big));
		assertFalse(small.lessOrEqualEgliMilner(big));
	}

	@Test
	public void testLessOrEqualEgliMilnerHoldsWhenBothConditionsAreSatisfied()
			throws SemanticException {
		TestPowerset s1 = of(new IntInterval(0, 2));
		TestPowerset s2 = of(new IntInterval(0, 5));
		assertTrue(s1.lessOrEqualEgliMilner(s2));
	}

	@Test
	public void testBottomIsLessOrEqualEgliMilnerToAnything()
			throws SemanticException {
		TestPowerset bottom = of();
		TestPowerset other = of(new IntInterval(0, 2));
		assertTrue(bottom.lessOrEqualEgliMilner(other));
	}

	@Test
	public void testWideningIsSoundWrtBothOperands()
			throws SemanticException {
		TestPowerset s1 = of(new IntInterval(0, 2));
		TestPowerset s2 = of(new IntInterval(0, 5));
		TestPowerset widened = s1.widening(s2);
		// a widening must be an upper bound of both operands
		assertTrue(s1.lessOrEqual(widened));
		assertTrue(s2.lessOrEqual(widened));
	}

	// --- EgliMilnerConnector edge cases (regression tests for a
	// NoSuchElementException previously thrown when only one of the two
	// operands was the empty set) ---

	@Test
	public void testEgliMilnerConnectorWithEmptyLeftOperandDoesNotThrow()
			throws SemanticException {
		TestPowerset empty = of();
		TestPowerset other = of(new IntInterval(3, 5));
		TestPowerset result = assertDoesNotThrow(() -> empty.EgliMilnerConnector(other));
		assertEquals(Set.of(new IntInterval(3, 5)), result.elements);
	}

	@Test
	public void testEgliMilnerConnectorWithEmptyRightOperandDoesNotThrow()
			throws SemanticException {
		TestPowerset nonEmpty = of(new IntInterval(3, 5));
		TestPowerset empty = of();
		TestPowerset result = assertDoesNotThrow(() -> nonEmpty.EgliMilnerConnector(empty));
		assertEquals(Set.of(new IntInterval(3, 5)), result.elements);
	}

	@Test
	public void testEgliMilnerConnectorWithBothOperandsEmptyIsEmpty()
			throws SemanticException {
		TestPowerset empty = of();
		TestPowerset result = empty.EgliMilnerConnector(empty);
		assertTrue(result.elements.isEmpty());
	}

}
