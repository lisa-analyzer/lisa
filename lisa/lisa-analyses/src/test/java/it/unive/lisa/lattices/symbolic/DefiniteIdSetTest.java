package it.unive.lisa.lattices.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DefiniteIdSetTest {

	private final Identifier x = new Variable(Int32Type.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Identifier y = new Variable(Int32Type.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private final Identifier z = new Variable(Int32Type.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private DefiniteIdSet set(
			Identifier... ids) {
		Set<Identifier> s = new HashSet<>();
		for (Identifier id : ids)
			s.add(id);
		return new DefiniteIdSet(s);
	}

	@Test
	public void emptySetOfElementsIsTop() {
		assertTrue(new DefiniteIdSet(Collections.emptySet()).isTop());
	}

	@Test
	public void nonEmptySetOfElementsIsNeitherTopNorBottom() {
		DefiniteIdSet s = set(x, y);
		assertFalse(s.isTop());
		assertFalse(s.isBottom());
	}

	@Test
	public void topHasNoElements() {
		assertTrue(new DefiniteIdSet(Collections.emptySet(), true).elements.isEmpty());
		assertTrue(new DefiniteIdSet(Collections.emptySet(), true).isTop());
	}

	@Test
	public void bottomHasNoElementsAndIsNotTop() {
		DefiniteIdSet bottom = new DefiniteIdSet(Collections.emptySet(), false);
		assertTrue(bottom.elements.isEmpty());
		assertFalse(bottom.isTop());
		assertTrue(bottom.isBottom());
	}

	@Test
	public void addYieldsANewSetWithTheExtraIdentifier() {
		DefiniteIdSet s = set(x);
		DefiniteIdSet added = s.add(y);
		assertTrue(added.elements.contains(x));
		assertTrue(added.elements.contains(y));
		// no side effect on the original set
		assertFalse(s.elements.contains(y));
	}

	@Test
	public void wideningToASupersetKeepsTheSuperset()
			throws SemanticException {
		DefiniteIdSet smaller = set(x);
		DefiniteIdSet bigger = set(x, y);
		assertEquals(bigger, smaller.wideningAux(bigger));
	}

	@Test
	public void wideningToANonSupersetGoesToTop()
			throws SemanticException {
		DefiniteIdSet s = set(x, y);
		DefiniteIdSet unrelated = set(z);
		assertTrue(s.wideningAux(unrelated).isTop());
	}

	@Test
	public void mkBuildsASetWithTheGivenElements() {
		DefiniteIdSet s = new DefiniteIdSet(Collections.emptySet(), true).mk(set(x).elements);
		assertEquals(set(x), s);
	}

}
