package it.unive.lisa.lattices.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AllocatedTypesTest {

	@Test
	public void emptySetIsTop() {
		assertTrue(new AllocatedTypes().isTop());
		assertTrue(new AllocatedTypes(Set.of()).isTop());
	}

	@Test
	public void bottomIsNotTop() {
		AllocatedTypes bottom = new AllocatedTypes().bottom();
		assertTrue(bottom.isBottom());
		assertFalse(bottom.isTop());
	}

	@Test
	public void nonEmptySetIsNeitherTopNorBottom() {
		AllocatedTypes types = new AllocatedTypes(Set.of("A"));
		assertFalse(types.isTop());
		assertFalse(types.isBottom());
	}

	@Test
	public void lessOrEqualIsSubsetOrTopOrBottom()
			throws SemanticException {
		AllocatedTypes a = new AllocatedTypes(Set.of("A"));
		AllocatedTypes ab = new AllocatedTypes(Set.of("A", "B"));
		AllocatedTypes top = new AllocatedTypes();
		AllocatedTypes bottom = top.bottom();

		assertTrue(a.lessOrEqual(ab));
		assertFalse(ab.lessOrEqual(a));
		assertTrue(a.lessOrEqual(top));
		assertTrue(bottom.lessOrEqual(a));
		assertTrue(top.lessOrEqual(top));
	}

	@Test
	public void lubIsUnion()
			throws SemanticException {
		AllocatedTypes a = new AllocatedTypes(Set.of("A"));
		AllocatedTypes b = new AllocatedTypes(Set.of("B"));
		assertEquals(new AllocatedTypes(Set.of("A", "B")), a.lub(b));
	}

	@Test
	public void lubWithTopIsTop()
			throws SemanticException {
		AllocatedTypes a = new AllocatedTypes(Set.of("A"));
		AllocatedTypes top = new AllocatedTypes();
		assertTrue(a.lub(top).isTop());
		assertTrue(top.lub(a).isTop());
	}

	@Test
	public void lubWithBottomIsIdentity()
			throws SemanticException {
		AllocatedTypes a = new AllocatedTypes(Set.of("A"));
		AllocatedTypes bottom = a.bottom();
		assertEquals(a, a.lub(bottom));
		assertEquals(a, bottom.lub(a));
	}

}
