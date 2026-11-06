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
 * Tests for {@link DefiniteSet}, whose documented semantics are: the partial
 * order is superset inclusion, the lub is set intersection, top is the empty
 * set, and bottom is (conceptually) the set of all elements.
 */
public class DefiniteSetTest {

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
	public void testTopIsTheEmptySet() {
		DefiniteSet<FakeElement> top = new DefiniteSet<>();
		assertTrue(top.isTop());
		assertFalse(top.isBottom());
		assertTrue(top.getDataflowElements().isEmpty());
	}

	@Test
	public void testBottomIsAlsoRepresentedAsAnEmptySetButWithADifferentFlag() {
		DefiniteSet<FakeElement> bottom = new DefiniteSet<>(false);
		assertTrue(bottom.isBottom());
		assertFalse(bottom.isTop());
	}

	@Test
	public void testLessOrEqualIsSupersetInclusion()
			throws SemanticException {
		DefiniteSet<FakeElement> bigger = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2)));
		DefiniteSet<FakeElement> smaller = new DefiniteSet<>(new HashSet<>(Set.of(e1)));
		// "definite" order: more known facts (a superset) is lower (more
		// precise) in the order
		assertTrue(bigger.lessOrEqual(smaller));
		assertFalse(smaller.lessOrEqual(bigger));
	}

	@Test
	public void testLubIsSetIntersection()
			throws SemanticException {
		DefiniteSet<FakeElement> a = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2)));
		DefiniteSet<FakeElement> b = new DefiniteSet<>(new HashSet<>(Set.of(e2, e3)));
		DefiniteSet<FakeElement> lub = a.lub(b);
		assertEquals(Set.of(e2), lub.getDataflowElements());
	}

	@Test
	public void testKnowsIdentifierChecksAllElements() {
		DefiniteSet<FakeElement> set = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2)));
		assertTrue(set.knowsIdentifier(x));
		assertTrue(set.knowsIdentifier(y));
		assertFalse(set.knowsIdentifier(z));
	}

	@Test
	public void testForgetIdentifierRemovesOnlyElementsInvolvingIt()
			throws SemanticException {
		DefiniteSet<FakeElement> set = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2)));
		DefiniteSet<FakeElement> after = set.forgetIdentifier(x, null);
		assertEquals(Set.of(e2), after.getDataflowElements());
	}

	@Test
	public void testForgetIdentifiersRemovesElementsInvolvingAnyGivenId()
			throws SemanticException {
		DefiniteSet<FakeElement> set = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2, e3)));
		DefiniteSet<FakeElement> after = set.forgetIdentifiers(Set.of(x, y), null);
		assertEquals(Set.of(e3), after.getDataflowElements());
	}

	@Test
	public void testForgetIdentifiersIfUsesThePredicateOnInvolvedIdentifiers()
			throws SemanticException {
		DefiniteSet<FakeElement> set = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2)));
		DefiniteSet<FakeElement> after = set.forgetIdentifiersIf(id -> id == x, null);
		assertEquals(Set.of(e2), after.getDataflowElements());
	}

	@Test
	public void testUpdateRemovesKilledElementsAndAddsGeneratedOnes() {
		DefiniteSet<FakeElement> set = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2)));
		DefiniteSet<FakeElement> updated = set.update(Set.of(e1), Set.of(e3));
		assertEquals(Set.of(e2, e3), updated.getDataflowElements());
	}

	@Test
	public void testStoreReplacesIdentifiersInsteadOfDuplicatingElements()
			throws SemanticException {
		// regression test: store(target, source) must produce exactly one
		// output element per input element, renaming occurrences of "source"
		// to "target" where relevant, and leaving the others untouched. A
		// previous bug (accidental shadowing of the "elements" field by a
		// same-named empty local variable) made this method always discard
		// every element and return an empty set.
		DefiniteSet<FakeElement> set = new DefiniteSet<>(new HashSet<>(Set.of(e1, e2)));
		DefiniteSet<FakeElement> after = set.store(z, x);

		assertEquals(2, after.getDataflowElements().size());
		FakeElement renamed = e1.replaceIdentifier(x, z);
		assertTrue(after.getDataflowElements().contains(renamed));
		assertTrue(after.getDataflowElements().contains(e2));
		assertFalse(after.getDataflowElements().contains(e1));
	}

	@Test
	public void testStoreOnElementsNotInvolvingSourceIsANoOp()
			throws SemanticException {
		DefiniteSet<FakeElement> set = new DefiniteSet<>(new HashSet<>(Set.of(e2)));
		DefiniteSet<FakeElement> after = set.store(z, x);
		assertEquals(Set.of(e2), after.getDataflowElements());
	}

	@Test
	public void testStoreOnTopOrBottomIsIdentity()
			throws SemanticException {
		DefiniteSet<FakeElement> top = new DefiniteSet<>();
		DefiniteSet<FakeElement> bottom = new DefiniteSet<>(false);
		assertSame(top, top.store(z, x));
		assertSame(bottom, bottom.store(z, x));
	}

}
