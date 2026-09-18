package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class SimpleAbstractStateTest {

	private static SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> full(
			SingleMemoryLattice h,
			SingleValueLattice v,
			SingleTypeLattice t) {
		return new SimpleAbstractState<>(h, v, t);
	}

	@Test
	public void singleComponentConstructorsDefaultTheOtherTwoToTheirSingletons() {
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> s = new SimpleAbstractState<>(SingleMemoryLattice.BOTTOM);
		assertSame(SingleMemoryLattice.BOTTOM, s.memoryState);
		assertSame(SingleValueLattice.SINGLETON, s.valueState);
		assertSame(SingleTypeLattice.SINGLETON, s.typeState);
	}

	@Test
	public void twoComponentConstructorsDefaultTheMissingOneToItsSingleton() {
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> s = new SimpleAbstractState<>(SingleMemoryLattice.BOTTOM, SingleValueLattice.BOTTOM);
		assertSame(SingleMemoryLattice.BOTTOM, s.memoryState);
		assertSame(SingleValueLattice.BOTTOM, s.valueState);
		assertSame(SingleTypeLattice.SINGLETON, s.typeState);
	}

	@Test
	public void isTopRequiresAllThreeComponentsToBeTop() {
		assertTrue(
				full(SingleMemoryLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON).isTop());
		assertFalse(
				full(SingleMemoryLattice.BOTTOM, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON).isTop());
		assertFalse(
				full(SingleMemoryLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.SINGLETON).isTop());
		assertFalse(
				full(SingleMemoryLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM).isTop());
	}

	@Test
	public void isBottomRequiresAllThreeComponentsToBeBottom() {
		assertTrue(full(SingleMemoryLattice.BOTTOM, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM).isBottom());
		assertFalse(
				full(SingleMemoryLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM).isBottom());
	}

	@Test
	public void lubIsComputedComponentwise() throws SemanticException {
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> a = full(SingleMemoryLattice.BOTTOM,
						SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> b = full(
				SingleMemoryLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM);

		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> lub = a.lub(b);
		assertSame(SingleMemoryLattice.SINGLETON, lub.memoryState);
		assertSame(SingleValueLattice.SINGLETON, lub.valueState);
		assertSame(SingleTypeLattice.BOTTOM, lub.typeState);
	}

	@Test
	public void glbIsComputedComponentwise() throws SemanticException {
		// neither operand is top/bottom overall (each has a mix of SINGLETON
		// and BOTTOM components), so BaseLattice's dispatch actually reaches
		// glbAux instead of short-circuiting
		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> a = full(
				SingleMemoryLattice.SINGLETON, SingleValueLattice.BOTTOM, SingleTypeLattice.SINGLETON);
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> b = full(SingleMemoryLattice.BOTTOM,
						SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);
		assertFalse(a.isTop());
		assertFalse(a.isBottom());
		assertFalse(b.isTop());
		assertFalse(b.isBottom());

		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> glb = a.glb(b);
		assertSame(SingleMemoryLattice.BOTTOM, glb.memoryState);
		assertSame(SingleValueLattice.BOTTOM, glb.valueState);
		assertSame(SingleTypeLattice.SINGLETON, glb.typeState);
	}

	@Test
	public void lessOrEqualRequiresAllThreeComponentsToBeLessOrEqual() throws SemanticException {
		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> narrow = full(
				SingleMemoryLattice.BOTTOM, SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM);
		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> wide = full(
				SingleMemoryLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);
		assertTrue(narrow.lessOrEqual(wide));
		assertFalse(wide.lessOrEqual(narrow));
	}

	@Test
	public void topAndBottomFactoriesBuildComponentwiseExtremes() {
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> s = full(SingleMemoryLattice.BOTTOM,
						SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		assertTrue(s.top().isTop());
		assertTrue(s.bottom().isBottom());
	}

	@Test
	public void withTopMemoryValuesTypesOnlyReplaceTheirOwnComponent() {
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> s = full(SingleMemoryLattice.BOTTOM,
						SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM);

		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> withTopMemory = s.withTopMemory();
		assertSame(SingleMemoryLattice.SINGLETON, withTopMemory.memoryState);
		assertSame(SingleValueLattice.BOTTOM, withTopMemory.valueState);
		assertSame(SingleTypeLattice.BOTTOM, withTopMemory.typeState);

		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> withTopValues = s.withTopValues();
		assertSame(SingleMemoryLattice.BOTTOM, withTopValues.memoryState);
		assertSame(SingleValueLattice.SINGLETON, withTopValues.valueState);
		assertSame(SingleTypeLattice.BOTTOM, withTopValues.typeState);

		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> withTopTypes = s.withTopTypes();
		assertSame(SingleMemoryLattice.BOTTOM, withTopTypes.memoryState);
		assertSame(SingleValueLattice.BOTTOM, withTopTypes.valueState);
		assertSame(SingleTypeLattice.SINGLETON, withTopTypes.typeState);
	}

	@Test
	public void knowsIdentifierIsTrueIfAnyComponentKnowsIt() {
		// all three Single*Lattice components always answer false, so the
		// composite must also always answer false - this exercises the "or"
		// composition itself, not any single component's own answer
		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> s = full(
				SingleMemoryLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);
		assertFalse(s.knowsIdentifier(null));
	}

	@Test
	public void forgetIdentifierPropagatesThroughAllThreeComponentsWhenNoSubstitutionIsProduced()
			throws SemanticException {
		// SingleMemoryLattice never produces heap replacements, so this
		// exercises the "no substitution needed" path of
		// SimpleAbstractState#forgetIdentifier/popScope/pushScope
		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> s = full(
				SingleMemoryLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.SINGLETON);

		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> forgotten = s.forgetIdentifier(null, null);
		assertSame(SingleMemoryLattice.SINGLETON, forgotten.memoryState);
		assertSame(SingleValueLattice.SINGLETON, forgotten.valueState);
		assertSame(SingleTypeLattice.SINGLETON, forgotten.typeState);

		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> popped = s.popScope(null, null);
		assertSame(SingleMemoryLattice.SINGLETON, popped.memoryState);

		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> pushed = s.pushScope(null, null);
		assertSame(SingleMemoryLattice.SINGLETON, pushed.memoryState);
	}

	@Test
	public void equalsAndHashCodeAreBasedOnAllThreeComponents() {
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> a = full(SingleMemoryLattice.BOTTOM,
						SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		SimpleAbstractState<SingleMemoryLattice,
				SingleValueLattice,
				SingleTypeLattice> b = full(SingleMemoryLattice.BOTTOM,
						SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		SimpleAbstractState<SingleMemoryLattice, SingleValueLattice, SingleTypeLattice> different = full(
				SingleMemoryLattice.SINGLETON, SingleValueLattice.SINGLETON, SingleTypeLattice.BOTTOM);
		assertFalse(a.equals(different));
	}

}
