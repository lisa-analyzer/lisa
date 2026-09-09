package it.unive.lisa.analysis.dataflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PossibleSet}, whose documented semantics are: the partial
 * order is subset inclusion, the lub is set union, bottom is the empty set, and
 * top is (conceptually) the set of all elements.
 */
public class PossibleSetTest {

	private static Identifier var(
			String name) {
		return new Variable(Untyped.INSTANCE, name, SyntheticLocation.INSTANCE);
	}

	private final Identifier x = var("x");
	private final Identifier y = var("y");
	private final Identifier z = var("z");

	private final FakeElement e1 = new FakeElement("e1", x);
	private final FakeElement e2 = new FakeElement("e2", y);
	private final FakeElement e3 = new FakeElement("e3", z);

	@Test
	public void testBottomIsTheEmptySet() {
		PossibleSet<FakeElement> bottom = new PossibleSet<>(false);
		assertTrue(bottom.isBottom());
		assertFalse(bottom.isTop());
		assertTrue(bottom.getDataflowElements().isEmpty());
	}

	@Test
	public void testTopIsAlsoRepresentedAsAnEmptySetButWithADifferentFlag() {
		PossibleSet<FakeElement> top = new PossibleSet<>();
		assertTrue(top.isTop());
		assertFalse(top.isBottom());
	}

	@Test
	public void testLessOrEqualIsSubsetInclusion()
			throws SemanticException {
		PossibleSet<FakeElement> smaller = new PossibleSet<>(new HashSet<>(Set.of(e1)));
		PossibleSet<FakeElement> bigger = new PossibleSet<>(new HashSet<>(Set.of(e1, e2)));
		assertTrue(smaller.lessOrEqual(bigger));
		assertFalse(bigger.lessOrEqual(smaller));
	}

	@Test
	public void testLubIsSetUnion()
			throws SemanticException {
		PossibleSet<FakeElement> a = new PossibleSet<>(new HashSet<>(Set.of(e1, e2)));
		PossibleSet<FakeElement> b = new PossibleSet<>(new HashSet<>(Set.of(e2, e3)));
		PossibleSet<FakeElement> lub = a.lub(b);
		assertEquals(Set.of(e1, e2, e3), lub.getDataflowElements());
	}

	@Test
	public void testKnowsIdentifierChecksAllElements() {
		PossibleSet<FakeElement> set = new PossibleSet<>(new HashSet<>(Set.of(e1, e2)));
		assertTrue(set.knowsIdentifier(x));
		assertTrue(set.knowsIdentifier(y));
		assertFalse(set.knowsIdentifier(z));
	}

	@Test
	public void testForgetIdentifierRemovesOnlyElementsInvolvingIt()
			throws SemanticException {
		PossibleSet<FakeElement> set = new PossibleSet<>(new HashSet<>(Set.of(e1, e2)));
		PossibleSet<FakeElement> after = set.forgetIdentifier(x, null);
		assertEquals(Set.of(e2), after.getDataflowElements());
	}

	@Test
	public void testForgetIdentifiersRemovesElementsInvolvingAnyGivenId()
			throws SemanticException {
		PossibleSet<FakeElement> set = new PossibleSet<>(new HashSet<>(Set.of(e1, e2, e3)));
		PossibleSet<FakeElement> after = set.forgetIdentifiers(Set.of(x, y), null);
		assertEquals(Set.of(e3), after.getDataflowElements());
	}

	@Test
	public void testForgetIdentifiersIfUsesThePredicateOnInvolvedIdentifiers()
			throws SemanticException {
		PossibleSet<FakeElement> set = new PossibleSet<>(new HashSet<>(Set.of(e1, e2)));
		PossibleSet<FakeElement> after = set.forgetIdentifiersIf(id -> id == x, null);
		assertEquals(Set.of(e2), after.getDataflowElements());
	}

	@Test
	public void testUpdateRemovesKilledElementsAndAddsGeneratedOnes() {
		PossibleSet<FakeElement> set = new PossibleSet<>(new HashSet<>(Set.of(e1, e2)));
		PossibleSet<FakeElement> updated = set.update(Set.of(e1), Set.of(e3));
		assertEquals(Set.of(e2, e3), updated.getDataflowElements());
	}

	@Test
	public void testStoreReplacesIdentifiersInsteadOfDuplicatingElements()
			throws SemanticException {
		// regression test: see DefiniteSetTest for the full rationale. The
		// same shadowing bug was present, verbatim, in PossibleSet.
		PossibleSet<FakeElement> set = new PossibleSet<>(new HashSet<>(Set.of(e1, e2)));
		PossibleSet<FakeElement> after = set.store(z, x);

		assertEquals(2, after.getDataflowElements().size());
		FakeElement renamed = e1.replaceIdentifier(x, z);
		assertTrue(after.getDataflowElements().contains(renamed));
		assertTrue(after.getDataflowElements().contains(e2));
		assertFalse(after.getDataflowElements().contains(e1));
	}

	@Test
	public void testStoreOnElementsNotInvolvingSourceIsANoOp()
			throws SemanticException {
		PossibleSet<FakeElement> set = new PossibleSet<>(new HashSet<>(Set.of(e2)));
		PossibleSet<FakeElement> after = set.store(z, x);
		assertEquals(Set.of(e2), after.getDataflowElements());
	}

	@Test
	public void testStoreOnTopOrBottomIsIdentity()
			throws SemanticException {
		PossibleSet<FakeElement> top = new PossibleSet<>();
		PossibleSet<FakeElement> bottom = new PossibleSet<>(false);
		assertSame(top, top.store(z, x));
		assertSame(bottom, bottom.store(z, x));
	}

}
