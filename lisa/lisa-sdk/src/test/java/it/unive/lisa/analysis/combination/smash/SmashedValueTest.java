package it.unive.lisa.analysis.combination.smash;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SingleTypeLattice;
import it.unive.lisa.lattices.SingleValueLattice;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link SmashedValue}, in particular the "kind" predicates
 * ({@link SmashedValue#isNumber()}, {@link SmashedValue#isString()},
 * {@link SmashedValue#isBool()}) that determine which of the three tracked
 * components is meaningful.
 */
public class SmashedValueTest {

	private SmashedValue<SingleValueLattice, SingleTypeLattice> number(
			SingleValueLattice v) {
		return new SmashedValue<>(v, SingleTypeLattice.BOTTOM, Satisfiability.BOTTOM);
	}

	private SmashedValue<SingleValueLattice, SingleTypeLattice> string(
			SingleTypeLattice v) {
		return new SmashedValue<>(SingleValueLattice.BOTTOM, v, Satisfiability.BOTTOM);
	}

	private SmashedValue<SingleValueLattice, SingleTypeLattice> bool(
			Satisfiability v) {
		return new SmashedValue<>(SingleValueLattice.BOTTOM, SingleTypeLattice.BOTTOM, v);
	}

	@Test
	public void testKindPredicatesForANumber() {
		SmashedValue<SingleValueLattice, SingleTypeLattice> n = number(SingleValueLattice.SINGLETON);

		assertTrue(n.isNumber());
		assertFalse(n.isString());
		assertFalse(n.isBool());
	}

	@Test
	public void testKindPredicatesForAString() {
		SmashedValue<SingleValueLattice, SingleTypeLattice> s = string(SingleTypeLattice.SINGLETON);

		assertFalse(s.isNumber());
		assertTrue(s.isString());
		assertFalse(s.isBool());
	}

	@Test
	public void testKindPredicatesForABool() {
		SmashedValue<SingleValueLattice, SingleTypeLattice> b = bool(Satisfiability.SATISFIED);

		assertFalse(b.isNumber());
		assertFalse(b.isString());
		assertTrue(b.isBool());
	}

	@Test
	public void testBottomIsNoKind() {
		SmashedValue<SingleValueLattice, SingleTypeLattice> bottom = new SmashedValue<>(SingleValueLattice.BOTTOM,
				SingleTypeLattice.BOTTOM, Satisfiability.BOTTOM);

		assertTrue(bottom.isBottom());
		assertFalse(bottom.isNumber());
		assertFalse(bottom.isString());
		assertFalse(bottom.isBool());
	}

	@Test
	public void testTopIsEveryKind() {
		SmashedValue<SingleValueLattice, SingleTypeLattice> top = new SmashedValue<>(SingleValueLattice.SINGLETON,
				SingleTypeLattice.SINGLETON, Satisfiability.UNKNOWN);

		assertTrue(top.isTop());
		assertTrue(top.isNumber());
		assertTrue(top.isString());
		assertTrue(top.isBool());
	}

	@Test
	public void testSameKind() {
		SmashedValue<SingleValueLattice, SingleTypeLattice> n1 = number(SingleValueLattice.SINGLETON);
		SmashedValue<SingleValueLattice, SingleTypeLattice> n2 = number(SingleValueLattice.SINGLETON);
		SmashedValue<SingleValueLattice, SingleTypeLattice> s = string(SingleTypeLattice.SINGLETON);

		assertTrue(n1.sameKind(n2));
		assertFalse(n1.sameKind(s));
	}

	@Test
	public void testLubOfSameKindPreservesTheKind()
			throws SemanticException {
		SmashedValue<SingleValueLattice, SingleTypeLattice> a = number(SingleValueLattice.BOTTOM);
		SmashedValue<SingleValueLattice, SingleTypeLattice> b = number(SingleValueLattice.SINGLETON);

		SmashedValue<SingleValueLattice, SingleTypeLattice> lub = a.lub(b);

		assertTrue(lub.isNumber());
		assertFalse(lub.isString());
	}

	@Test
	public void testLubOfDifferentKindsLosesTheKind()
			throws SemanticException {
		// mixing a number and a string produces a value where both components
		// are non-bottom: it no longer represents a single, precise kind (it is
		// not top either, since the boolean component is still bottom)
		SmashedValue<SingleValueLattice, SingleTypeLattice> n = number(SingleValueLattice.SINGLETON);
		SmashedValue<SingleValueLattice, SingleTypeLattice> s = string(SingleTypeLattice.SINGLETON);

		SmashedValue<SingleValueLattice, SingleTypeLattice> lub = n.lub(s);

		assertFalse(lub.isTop());
		assertFalse(lub.isNumber());
		assertFalse(lub.isString());
		assertFalse(lub.isBool());
	}

}
