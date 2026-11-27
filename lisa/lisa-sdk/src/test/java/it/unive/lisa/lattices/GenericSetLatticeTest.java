package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GenericSetLatticeTest {

	private static GenericSetLattice<String> set(
			String... elems) {
		return new GenericSetLattice<>(new HashSet<>(Arrays.asList(elems)));
	}

	@Test
	public void addReturnsANewSetWithoutModifyingTheReceiver() {
		GenericSetLattice<String> original = set("a");
		GenericSetLattice<String> added = original.add("b");
		assertEquals(set("a"), original);
		assertEquals(set("a", "b"), added);
	}

	@Test
	public void addAllReturnsANewSetWithoutModifyingTheReceiver() {
		GenericSetLattice<String> original = set("a");
		GenericSetLattice<String> added = original.addAll(List.of("b", "c"));
		assertEquals(set("a"), original);
		assertEquals(set("a", "b", "c"), added);
	}

	@Test
	public void removeReturnsANewSetWithoutModifyingTheReceiver() {
		GenericSetLattice<String> original = set("a", "b");
		GenericSetLattice<String> removed = original.remove("a");
		assertEquals(set("a", "b"), original);
		assertEquals(set("b"), removed);
	}

	@Test
	public void removeAllReturnsANewSetWithoutModifyingTheReceiver() {
		GenericSetLattice<String> original = set("a", "b", "c");
		GenericSetLattice<String> removed = original.removeAll(List.of("a", "b"));
		assertEquals(set("a", "b", "c"), original);
		assertEquals(set("c"), removed);
	}

	@Test
	public void removingANonExistingElementIsANoOp() {
		GenericSetLattice<String> original = set("a");
		assertEquals(original, original.remove("z"));
	}

	@Test
	public void twoArgConstructorAllowsAnExplicitTopEmptySet() {
		GenericSetLattice<String> explicitTop = new GenericSetLattice<>(new HashSet<>(), true);
		assertTrue(explicitTop.isTop());
		assertFalse(explicitTop.isBottom());
	}

}
