package it.unive.lisa.lattices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GenericMapLatticeTest {

	private static GenericMapLattice<String, SingleValueLattice> top() {
		return new GenericMapLattice<>(SingleValueLattice.SINGLETON);
	}

	private static GenericMapLattice<String, SingleValueLattice> bottom() {
		return new GenericMapLattice<>(SingleValueLattice.BOTTOM);
	}

	@Test
	public void aFreshInstanceWithATopLatticeAndNoFunctionIsTop() {
		assertTrue(top().isTop());
		assertFalse(top().isBottom());
	}

	@Test
	public void aFreshInstanceWithABottomLatticeAndNoFunctionIsBottom() {
		assertTrue(bottom().isBottom());
		assertFalse(bottom().isTop());
	}

	@Test
	public void anExplicitlyEmptyFunctionIsNormalizedToNull() {
		// FunctionalLattice's 2-arg constructor treats a non-null-but-empty
		// map the same as null: isTop()/isBottom() only look at
		// "function == null", so an empty map must collapse to null for the
		// top/bottom checks to remain meaningful
		GenericMapLattice<String, SingleValueLattice> m = new GenericMapLattice<>(
				SingleValueLattice.SINGLETON, PatriciaTrieMap.empty());
		assertNull(m.function);
		assertTrue(m.isTop());
	}

	@Test
	public void putStateMakesTheMapNeitherTopNorBottom() {
		GenericMapLattice<String, SingleValueLattice> m = top().putState("x", SingleValueLattice.SINGLETON);
		assertFalse(m.isTop());
		assertFalse(m.isBottom());
		assertEquals(SingleValueLattice.SINGLETON, m.getState("x"));
	}

	@Test
	public void getStateOfATopMapIsAlwaysLatticeTop() {
		assertEquals(SingleValueLattice.SINGLETON, top().getState("anything"));
	}

	@Test
	public void getStateOfABottomMapIsAlwaysLatticeBottom() {
		assertEquals(SingleValueLattice.BOTTOM, bottom().getState("anything"));
	}

	@Test
	public void getStateOfAnUnmappedKeyIsStateOfUnknown() {
		// GenericMapLattice#stateOfUnknown always returns lattice.bottom()
		GenericMapLattice<String, SingleValueLattice> m = top().putState("x", SingleValueLattice.SINGLETON);
		assertEquals(SingleValueLattice.BOTTOM, m.getState("y"));
	}

	@Test
	public void getOrDefaultUsesTheGivenDefaultInsteadOfStateOfUnknown() {
		GenericMapLattice<String, SingleValueLattice> m = top().putState("x", SingleValueLattice.SINGLETON);
		assertEquals(SingleValueLattice.SINGLETON, m.getOtDefault("y", SingleValueLattice.SINGLETON));
	}

	@Test
	public void removeDropsAKeyAndCollapsesToNullWhenTheResultIsEmpty() {
		GenericMapLattice<String, SingleValueLattice> m = top().putState("x", SingleValueLattice.SINGLETON);
		GenericMapLattice<String, SingleValueLattice> removed = m.remove("x");
		assertNull(removed.function);
		assertTrue(removed.isTop());
	}

	@Test
	public void removeOnATopOrBottomMapIsANoOp() {
		GenericMapLattice<String, SingleValueLattice> t = top();
		GenericMapLattice<String, SingleValueLattice> b = bottom();
		assertSame(t, t.remove("x"));
		assertSame(b, b.remove("x"));
	}

	@Test
	public void removeAllDropsMultipleKeys() {
		GenericMapLattice<String, SingleValueLattice> m = top()
				.putState("x", SingleValueLattice.SINGLETON)
				.putState("y", SingleValueLattice.SINGLETON)
				.putState("z", SingleValueLattice.SINGLETON);
		GenericMapLattice<String, SingleValueLattice> removed = m.removeAll(List.of("x", "y"));
		assertEquals(Map.of("z", SingleValueLattice.SINGLETON), removed.function.toHashMap());
	}

	// regression test: removeAllMatching used to call
	// function.keySet().removeIf(test) instead of
	// result.keySet().removeIf(test),
	// which mutated the receiver's own (shared) map in place and left the
	// returned copy completely untouched, so the matching keys were never
	// actually removed from what callers received
	@Test
	public void removeAllMatchingDropsKeysAndDoesNotMutateTheReceiver() {
		GenericMapLattice<String, SingleValueLattice> m = top()
				.putState("keep", SingleValueLattice.SINGLETON)
				.putState("drop1", SingleValueLattice.SINGLETON)
				.putState("drop2", SingleValueLattice.SINGLETON);

		GenericMapLattice<String, SingleValueLattice> result = m.removeAllMatching(k -> k.startsWith("drop"));

		assertEquals(Map.of("keep", SingleValueLattice.SINGLETON), result.function.toHashMap());
		// the receiver must be untouched
		assertEquals(3, m.function.size());
		assertTrue(m.function.containsKey("drop1"));
	}

	@Test
	public void removeAllMatchingCollapsesToNullWhenEverythingIsRemoved() {
		GenericMapLattice<String, SingleValueLattice> m = top().putState("x", SingleValueLattice.SINGLETON);
		GenericMapLattice<String, SingleValueLattice> result = m.removeAllMatching(k -> true);
		assertNull(result.function);
		assertTrue(result.isTop());
	}

	@Test
	public void lubMergesKeysAndLubsValuesUsingBottomForMissingOnes() throws SemanticException {
		GenericMapLattice<String, SingleValueLattice> a = new GenericMapLattice<String, SingleValueLattice>(
				SingleValueLattice.BOTTOM).putState("x", SingleValueLattice.SINGLETON);
		GenericMapLattice<String, SingleValueLattice> b = new GenericMapLattice<String, SingleValueLattice>(
				SingleValueLattice.BOTTOM).putState("y", SingleValueLattice.SINGLETON);

		GenericMapLattice<String, SingleValueLattice> lub = a.lub(b);
		assertEquals(Map.of("x", SingleValueLattice.SINGLETON, "y", SingleValueLattice.SINGLETON),
				lub.function.toHashMap());
	}

	@Test
	public void lessOrEqualRequiresEveryMappedKeyToBeLessOrEqualInTheOther() throws SemanticException {
		GenericMapLattice<String, SingleValueLattice> narrow = new GenericMapLattice<String, SingleValueLattice>(
				SingleValueLattice.BOTTOM).putState("x", SingleValueLattice.BOTTOM);
		GenericMapLattice<String, SingleValueLattice> wide = new GenericMapLattice<String, SingleValueLattice>(
				SingleValueLattice.BOTTOM).putState("x", SingleValueLattice.SINGLETON);
		assertTrue(narrow.lessOrEqual(wide));
		assertFalse(wide.lessOrEqual(narrow));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnFunctionAndLattice() {
		GenericMapLattice<String, SingleValueLattice> a = top().putState("x", SingleValueLattice.SINGLETON);
		GenericMapLattice<String, SingleValueLattice> b = top().putState("x", SingleValueLattice.SINGLETON);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(top()));
	}

}
