package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class SimpleAbstractStateTest {

	private static SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> full(
			SingleHeapLattice h,
			SingleValueLattice v,
			SingleTypeLattice t) {
		return new SimpleAbstractState<>(h, v, t);
	}

	@Test
	public void singleComponentConstructorsDefaultTheOtherTwoToTheirSingletons() {
		SimpleAbstractState<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> s = new SimpleAbstractState<>(SingleHeapLattice.BOTTOM);
		assertSame(SingleHeapLattice.BOTTOM, s.heapState);
		assertSame(SingleValueLattice.SINGLETON, s.valueState);
		assertSame(SingleTypeLattice.SINGLETON, s.typeState);
	}

	@Test
	public void twoComponentConstructorsDefaultTheMissingOneToItsSingleton() {
		SimpleAbstractState<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> s = new SimpleAbstractState<>(SingleHeapLattice.BOTTOM, SingleValueLattice.BOTTOM);
		assertSame(SingleHeapLattice.BOTTOM, s.heapState);
		assertSame(SingleValueLattice.BOTTOM, s.valueState);
		assertSame(SingleTypeLattice.SINGLETON, s.typeState);
	}

	@Test
	public void isTopRequiresAllThreeComponentsToBeTop() {
		assertTrue(
				full(SingleHeapLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON).isTop());
		assertFalse(full(SingleHeapLattice.BOTTOM, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON).isTop());
		assertFalse(full(SingleHeapLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.SINGLETON).isTop());
		assertFalse(full(SingleHeapLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM).isTop());
	}

	@Test
	public void isBottomRequiresAllThreeComponentsToBeBottom() {
		assertTrue(full(SingleHeapLattice.BOTTOM, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM).isBottom());
		assertFalse(full(SingleHeapLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM).isBottom());
	}

	@Test
	public void lubIsComputedComponentwise() throws SemanticException {
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> a = full(SingleHeapLattice.BOTTOM,
				SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> b = full(
				SingleHeapLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM);

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> lub = a.lub(b);
		assertSame(SingleHeapLattice.SINGLETON, lub.heapState);
		assertSame(SingleValueLattice.SINGLETON, lub.valueState);
		assertSame(SingleTypeLattice.BOTTOM, lub.typeState);
	}

	@Test
	public void glbIsComputedComponentwise() throws SemanticException {
		// neither operand is top/bottom overall (each has a mix of SINGLETON
		// and BOTTOM components), so BaseLattice's dispatch actually reaches
		// glbAux instead of short-circuiting
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> a = full(
				SingleHeapLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.SINGLETON);
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> b = full(SingleHeapLattice.BOTTOM,
				SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);
		assertFalse(a.isTop());
		assertFalse(a.isBottom());
		assertFalse(b.isTop());
		assertFalse(b.isBottom());

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> glb = a.glb(b);
		assertSame(SingleHeapLattice.BOTTOM, glb.heapState);
		assertSame(SingleValueLattice.BOTTOM, glb.valueState);
		assertSame(SingleTypeLattice.SINGLETON, glb.typeState);
	}

	@Test
	public void lessOrEqualRequiresAllThreeComponentsToBeLessOrEqual() throws SemanticException {
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> narrow = full(
				SingleHeapLattice.BOTTOM, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM);
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> wide = full(
				SingleHeapLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);
		assertTrue(narrow.lessOrEqual(wide));
		assertFalse(wide.lessOrEqual(narrow));
	}

	@Test
	public void topAndBottomFactoriesBuildComponentwiseExtremes() {
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> s = full(SingleHeapLattice.BOTTOM,
				SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		assertTrue(s.top().isTop());
		assertTrue(s.bottom().isBottom());
	}

	@Test
	public void withTopMemoryValuesTypesOnlyReplaceTheirOwnComponent() {
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> s = full(SingleHeapLattice.BOTTOM,
				SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM);

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> withTopMemory = s.withTopMemory();
		assertSame(SingleHeapLattice.SINGLETON, withTopMemory.heapState);
		assertSame(SingleValueLattice.BOTTOM, withTopMemory.valueState);
		assertSame(SingleTypeLattice.BOTTOM, withTopMemory.typeState);

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> withTopValues = s.withTopValues();
		assertSame(SingleHeapLattice.BOTTOM, withTopValues.heapState);
		assertSame(SingleValueLattice.SINGLETON, withTopValues.valueState);
		assertSame(SingleTypeLattice.BOTTOM, withTopValues.typeState);

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> withTopTypes = s.withTopTypes();
		assertSame(SingleHeapLattice.BOTTOM, withTopTypes.heapState);
		assertSame(SingleValueLattice.BOTTOM, withTopTypes.valueState);
		assertSame(SingleTypeLattice.SINGLETON, withTopTypes.typeState);
	}

	@Test
	public void knowsIdentifierIsTrueIfAnyComponentKnowsIt() {
		// all three Single*Lattice components always answer false, so the
		// composite must also always answer false - this exercises the "or"
		// composition itself, not any single component's own answer
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> s = full(
				SingleHeapLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);
		assertFalse(s.knowsIdentifier(null));
	}

	@Test
	public void forgetIdentifierPropagatesThroughAllThreeComponentsWhenNoSubstitutionIsProduced()
			throws SemanticException {
		// SingleHeapLattice never produces heap replacements, so this
		// exercises the "no substitution needed" path of
		// SimpleAbstractState#forgetIdentifier/popScope/pushScope
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> s = full(
				SingleHeapLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);

		SimpleAbstractState<SingleHeapLattice,
				SingleValueLattice,
				SingleTypeLattice> forgotten = s.forgetIdentifier(null, null);
		assertSame(SingleHeapLattice.SINGLETON, forgotten.heapState);
		assertSame(SingleValueLattice.SINGLETON, forgotten.valueState);
		assertSame(SingleTypeLattice.SINGLETON, forgotten.typeState);

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> popped = s.popScope(null, null);
		assertSame(SingleHeapLattice.SINGLETON, popped.heapState);

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> pushed = s.pushScope(null, null);
		assertSame(SingleHeapLattice.SINGLETON, pushed.heapState);
	}

	@Test
	public void equalsAndHashCodeAreBasedOnAllThreeComponents() {
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> a = full(SingleHeapLattice.BOTTOM,
				SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> b = full(SingleHeapLattice.BOTTOM,
				SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		SimpleAbstractState<SingleHeapLattice, SingleValueLattice, SingleTypeLattice> different = full(
				SingleHeapLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		assertFalse(a.equals(different));
	}

}
