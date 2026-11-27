package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

// exercises the shared InverseSetLattice logic (lub=intersection,
// glb=union, lessOrEqual=superset) through the concrete
// GenericInverseSetLattice
public class InverseSetLatticeTest {

	private static GenericInverseSetLattice<String> set(
			String... elems) {
		return new GenericInverseSetLattice<>(new HashSet<>(Arrays.asList(elems)));
	}

	@Test
	public void topIsTheEmptySet() {
		GenericInverseSetLattice<String> top = new GenericInverseSetLattice<String>().top();
		assertTrue(top.isTop());
		assertTrue(top.isEmpty());
	}

	@Test
	public void bottomIsTheEmptySetSentinel() {
		GenericInverseSetLattice<String> bottom = new GenericInverseSetLattice<String>().bottom();
		assertTrue(bottom.isBottom());
		assertTrue(bottom.isEmpty());
	}

	@Test
	public void lubIsSetIntersection() throws SemanticException {
		GenericInverseSetLattice<String> result = set("a", "b").lub(set("b", "c"));
		assertEquals(set("b"), result);
	}

	@Test
	public void lubOfDisjointSetsIsTop() throws SemanticException {
		// unlike the normal SetLattice, here the empty set produced by
		// intersecting two disjoint sets IS mathematically top: in the
		// inverse (superset) order, the empty set is a superset of nothing
		// except itself, making it the greatest element
		GenericInverseSetLattice<String> result = set("a").lub(set("b"));
		assertTrue(result.isTop());
	}

	@Test
	public void glbIsSetUnion() throws SemanticException {
		GenericInverseSetLattice<String> result = set("a", "b").glb(set("b", "c"));
		assertEquals(set("a", "b", "c"), result);
	}

	@Test
	public void lessOrEqualIsSupersetInclusion() throws SemanticException {
		assertTrue(set("a", "b").lessOrEqual(set("a")));
		assertFalse(set("a").lessOrEqual(set("a", "b")));
		assertTrue(set("a").lessOrEqual(set("a")));
	}

	@Test
	public void containsReflectsMembership() {
		GenericInverseSetLattice<String> s = set("a", "b");
		assertTrue(s.contains("a"));
		assertFalse(s.contains("z"));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnElementsAndTopFlag() {
		assertEquals(set("a", "b"), set("b", "a"));
		assertEquals(set("a", "b").hashCode(), set("b", "a").hashCode());
	}

}
