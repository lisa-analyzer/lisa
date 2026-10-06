package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.Test;

// exercises the shared SetLattice logic (lub=union, glb=intersection,
// lessOrEqual=subset) through the concrete GenericSetLattice
public class SetLatticeTest {

	private static GenericSetLattice<String> set(
			String... elems) {
		return new GenericSetLattice<>(new HashSet<>(Arrays.asList(elems)));
	}

	@Test
	public void topIsTheEmptySetWithTheTopFlag() {
		GenericSetLattice<String> top = new GenericSetLattice<String>().top();
		assertTrue(top.isTop());
		assertTrue(top.isEmpty());
	}

	@Test
	public void bottomIsTheEmptySetWithoutTheTopFlag() {
		GenericSetLattice<String> bottom = new GenericSetLattice<String>().bottom();
		assertTrue(bottom.isBottom());
		assertTrue(bottom.isEmpty());
	}

	@Test
	public void aNonEmptySetIsNeitherTopNorBottom() {
		GenericSetLattice<String> s = set("a");
		assertFalse(s.isTop());
		assertFalse(s.isBottom());
	}

	@Test
	public void lubIsSetUnion() throws SemanticException {
		GenericSetLattice<String> result = set("a", "b").lub(set("b", "c"));
		assertEquals(set("a", "b", "c"), result);
	}

	@Test
	public void glbIsSetIntersection() throws SemanticException {
		GenericSetLattice<String> result = set("a", "b").glb(set("b", "c"));
		assertEquals(set("b"), result);
	}

	@Test
	public void glbOfDisjointSetsIsBottom() throws SemanticException {
		// regression test: the empty set is mathematically the least element
		// of a normal (non-inverse) subset lattice (it is a subset of every
		// other set), so the intersection of two disjoint, non-top/non-bottom
		// sets must be bottom - a GenericSetLattice(Set) constructor bug used
		// to make this (and GenericSetLattice#remove() emptying a singleton)
		// silently become top instead
		GenericSetLattice<String> result = set("a").glb(set("b"));
		assertTrue(result.isEmpty());
		assertTrue(result.isBottom());
		assertFalse(result.isTop());
	}

	@Test
	public void removingTheLastElementYieldsBottomNotTop() {
		// regression test, see glbOfDisjointSetsIsBottom above
		GenericSetLattice<String> result = set("a").remove("a");
		assertTrue(result.isEmpty());
		assertTrue(result.isBottom());
		assertFalse(result.isTop());
	}

	@Test
	public void lessOrEqualIsSubsetInclusion() throws SemanticException {
		assertTrue(set("a").lessOrEqual(set("a", "b")));
		assertFalse(set("a", "b").lessOrEqual(set("a")));
		assertTrue(set("a").lessOrEqual(set("a")));
	}

	@Test
	public void containsReflectsMembership() {
		GenericSetLattice<String> s = set("a", "b");
		assertTrue(s.contains("a"));
		assertFalse(s.contains("z"));
	}

	@Test
	public void sizeAndIsEmptyReflectTheElementsSet() {
		assertEquals(2, set("a", "b").size());
		assertTrue(new GenericSetLattice<String>().bottom().isEmpty());
		assertFalse(set("a").isEmpty());
	}

	@Test
	public void iteratorWalksTheElements() {
		GenericSetLattice<String> s = set("a", "b");
		Set<String> seen = new HashSet<>();
		Iterator<String> it = s.iterator();
		while (it.hasNext())
			seen.add(it.next());
		assertEquals(Set.of("a", "b"), seen);
	}

	@Test
	public void equalsAndHashCodeAreBasedOnElementsAndTopFlag() {
		assertEquals(set("a", "b"), set("b", "a"));
		assertEquals(set("a", "b").hashCode(), set("b", "a").hashCode());
	}

	@Test
	public void toStringRendersTopAndBottomSpecially() {
		assertEquals("#TOP#", new GenericSetLattice<String>().top().toString());
		assertEquals("_|_", new GenericSetLattice<String>().bottom().toString());
		assertEquals(set("a").elements.toString(), set("a").toString());
	}

}
