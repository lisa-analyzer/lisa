package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.lattices.GenericSetLattice;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class FixpointInfoTest {

	private static GenericSetLattice<String> set(
			String... elements) {
		return new GenericSetLattice<>(Set.of(elements));
	}

	@Test
	public void defaultConstructorIsTop() {
		FixpointInfo info = new FixpointInfo();
		assertTrue(info.isTop());
		assertFalse(info.isBottom());
		assertTrue(info.isEmpty());
	}

	@Test
	public void constructorWithEmptyMapIsAlsoTop() {
		// an empty (but non-null) mapping carries no information, and is thus
		// normalized to the same representation as the default constructor
		FixpointInfo info = new FixpointInfo(Map.of());
		assertTrue(info.isTop());
	}

	@Test
	public void bottomIsUniqueAndDistinctFromTop() {
		FixpointInfo bottom = FixpointInfo.BOTTOM;
		assertTrue(bottom.isBottom());
		assertFalse(bottom.isTop());
		assertEquals(FixpointInfo.BOTTOM, new FixpointInfo().bottom());
	}

	@Test
	public void getOnMissingKeyReturnsNull() {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertNull(info.get("missing"));
	}

	@Test
	public void getOnTopOrBottomReturnsNullRegardlessOfKey() {
		assertNull(new FixpointInfo().get("anything"));
		assertNull(FixpointInfo.BOTTOM.get("anything"));
	}

	@Test
	public void getWithTypeCastsTheValue() {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		GenericSetLattice<?> value = info.get("a", GenericSetLattice.class);
		assertEquals(set("x"), value);
	}

	@Test
	public void putIsAStrongUpdateThatDiscardsThePreviousValue() {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		FixpointInfo updated = info.put("a", set("y"));
		// the value stored is exactly the new one, not a lub with the old one
		assertEquals(set("y"), updated.get("a"));
	}

	@Test
	public void putOnAFreshKeyAddsIt() {
		FixpointInfo info = new FixpointInfo();
		FixpointInfo updated = info.put("a", set("x"));
		assertFalse(updated.isTop());
		assertEquals(set("x"), updated.get("a"));
	}

	@Test
	public void putWeakOnAFreshKeyStoresTheGivenValueWithoutThrowing()
			throws SemanticException {
		// regression test: putWeak used to unconditionally dereference the
		// (possibly absent) previous mapping when comparing its runtime type
		// against the new value, causing a NullPointerException whenever the
		// key was not already present (including when this instance is top,
		// as get() always returns null in that case)
		FixpointInfo info = new FixpointInfo();
		FixpointInfo updated = info.putWeak("a", set("x"));
		assertEquals(set("x"), updated.get("a"));
	}

	@Test
	public void putWeakOnAnExistingKeyOfTheSameTypeLubsTheValues()
			throws SemanticException {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		FixpointInfo updated = info.putWeak("a", set("y"));
		assertEquals(set("x", "y"), updated.get("a"));
	}

	@Test
	public void putWeakOnAnExistingKeyOfADifferentTypeThrows() {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertThrows(IllegalArgumentException.class, () -> info.putWeak("a", FixpointInfo.BOTTOM));
	}

	@Test
	public void lubUnionsKeysAndLubsCommonOnes()
			throws SemanticException {
		FixpointInfo left = new FixpointInfo(Map.of("a", set("x"), "common", set("1")));
		FixpointInfo right = new FixpointInfo(Map.of("b", set("y"), "common", set("2")));
		FixpointInfo lub = left.lub(right);
		assertEquals(set("x"), lub.get("a"));
		assertEquals(set("y"), lub.get("b"));
		assertEquals(set("1", "2"), lub.get("common"));
	}

	@Test
	public void glbIntersectsKeysAndGlbsCommonOnes()
			throws SemanticException {
		FixpointInfo left = new FixpointInfo(Map.of("a", set("x"), "common", set("1", "2")));
		FixpointInfo right = new FixpointInfo(Map.of("b", set("y"), "common", set("2", "3")));
		FixpointInfo glb = left.glb(right);
		assertNull(glb.get("a"));
		assertNull(glb.get("b"));
		assertEquals(set("2"), glb.get("common"));
	}

	@Test
	public void lessOrEqualRequiresEveryKeyOfThisToBeLessOrEqualInOther()
			throws SemanticException {
		FixpointInfo smaller = new FixpointInfo(Map.of("a", set("x")));
		FixpointInfo bigger = new FixpointInfo(Map.of("a", set("x", "y"), "b", set("z")));
		assertTrue(smaller.lessOrEqual(bigger));
		assertFalse(bigger.lessOrEqual(smaller));
	}

	@Test
	public void lessOrEqualIsFalseWhenOtherIsMissingAKey()
			throws SemanticException {
		FixpointInfo left = new FixpointInfo(Map.of("a", set("x")));
		FixpointInfo right = new FixpointInfo(Map.of("b", set("y")));
		assertFalse(left.lessOrEqual(right));
	}

	@Test
	public void topIsTheNeutralElementOfLub()
			throws SemanticException {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertTrue(info.lub(info.top()).isTop());
	}

	@Test
	public void bottomIsTheNeutralElementOfLub()
			throws SemanticException {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertEquals(info, info.lub(FixpointInfo.BOTTOM));
	}

	@Test
	public void keysAndValuesReflectTheMapping() {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertEquals(Set.of("a"), info.getKeys());
		assertTrue(info.getValues().contains(set("x")));
		assertEquals(Map.of("a", set("x")), info.getMap());
	}

	@Test
	public void keysAndValuesAreEmptyWhenTopOrBottom() {
		assertTrue(new FixpointInfo().getKeys().isEmpty());
		assertTrue(new FixpointInfo().getValues().isEmpty());
		assertTrue(FixpointInfo.BOTTOM.getKeys().isEmpty());
	}

	@Test
	public void isEmptyHoldsForTopBottomAndTrulyEmptyMappings() {
		assertTrue(new FixpointInfo().isEmpty());
		assertTrue(FixpointInfo.BOTTOM.isEmpty());
		assertTrue(new FixpointInfo(Map.of()).isEmpty());
		assertFalse(new FixpointInfo(Map.of("a", set("x"))).isEmpty());
	}

	@Test
	public void iteratorWalksAllEntries() {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x"), "b", set("y")));
		Map<String, Lattice<?>> collected = new HashMap<>();
		info.forEach(e -> collected.put(e.getKey(), e.getValue()));
		assertEquals(Map.of("a", set("x"), "b", set("y")), collected);
	}

	@Test
	public void iteratorIsEmptyWhenTop() {
		assertFalse(new FixpointInfo().iterator().hasNext());
	}

}
