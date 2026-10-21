package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.lattices.SetLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class FixpointInfoTest {

	private static GenericSetLattice<String> set(
			String... elements) {
		return new GenericSetLattice<>(Set.of(elements));
	}

	/**
	 * A set of strings that records the scoping operations applied to it.
	 */
	private static class ScopedSet
			extends
			SetLattice<ScopedSet, String>
			implements
			ScopedObject<ScopedSet> {

		private ScopedSet(
				Set<String> elements,
				boolean isTop) {
			super(elements, isTop);
		}

		@Override
		public ScopedSet top() {
			return new ScopedSet(Set.of(), true);
		}

		@Override
		public ScopedSet bottom() {
			return new ScopedSet(Set.of(), false);
		}

		@Override
		public ScopedSet mk(
				Set<String> set) {
			return new ScopedSet(set, false);
		}

		private ScopedSet with(
				String element) {
			Set<String> result = new HashSet<>(elements);
			result.add(element);
			return mk(result);
		}

		@Override
		public ScopedSet pushScope(
				ScopeToken token,
				ProgramPoint pp) {
			return with("push");
		}

		@Override
		public ScopedSet popScope(
				ScopeToken token,
				ProgramPoint pp) {
			return with("pop");
		}

	}

	@Test
	public void defaultConstructorIsEmptyButNotTop() {
		FixpointInfo info = new FixpointInfo();
		assertFalse(info.isTop());
		assertFalse(info.isBottom());
		assertTrue(info.isEmpty());
	}

	@Test
	public void constructorWithEmptyMapIsEquivalentToDefaultConstructor() {
		// an empty (but non-null) mapping carries no information, and is thus
		// normalized to the same representation as the default constructor
		FixpointInfo info = new FixpointInfo(Map.of());
		assertEquals(new FixpointInfo(), info);
		assertFalse(info.isTop());
	}

	@Test
	public void bottomIsUniqueAndDistinctFromTop() {
		FixpointInfo bottom = FixpointInfo.BOTTOM;
		assertTrue(bottom.isBottom());
		assertFalse(bottom.isTop());
		assertEquals(FixpointInfo.BOTTOM, new FixpointInfo().bottom());
	}

	@Test
	public void topIsUniqueAndDistinctFromBottom() {
		FixpointInfo top = FixpointInfo.TOP;
		assertTrue(top.isTop());
		assertFalse(top.isBottom());
		assertSame(FixpointInfo.TOP, new FixpointInfo().top());
		assertNotEquals(new FixpointInfo(), FixpointInfo.TOP);
	}

	@Test
	public void emptyIsTheNeutralElementOfLub()
			throws SemanticException {
		// regression test: the empty mapping used to be considered top, and the
		// lub with it used to discard all the information of the other operand
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertEquals(info, new FixpointInfo().lub(info));
		assertEquals(info, info.lub(new FixpointInfo()));
		assertEquals(info, new FixpointInfo().upchain(info));
		assertEquals(info, new FixpointInfo().widening(info));
	}

	@Test
	public void emptyIsLessOrEqualThanNonEmpty()
			throws SemanticException {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertTrue(new FixpointInfo().lessOrEqual(info));
		assertFalse(info.lessOrEqual(new FixpointInfo()));
		assertTrue(info.lessOrEqual(FixpointInfo.TOP));
	}

	@Test
	public void scopingIsForwardedOnlyToScopedValues()
			throws SemanticException {
		// the program point is not used by the scoping operations under test
		ProgramPoint pp = null;
		ScopeToken token = new ScopeToken(() -> SyntheticLocation.INSTANCE);
		ScopedSet scoped = new ScopedSet(Set.of(), false);
		FixpointInfo info = new FixpointInfo(Map.of("scoped", scoped, "plain", set("x")));

		FixpointInfo pushed = info.pushScope(token, pp);
		assertEquals(scoped.with("push"), pushed.get("scoped"));
		assertEquals(set("x"), pushed.get("plain"));

		FixpointInfo popped = pushed.popScope(token, pp);
		assertEquals(scoped.with("push").with("pop"), popped.get("scoped"));
		assertEquals(set("x"), popped.get("plain"));
	}

	@Test
	public void scopingWithoutScopedValuesYieldsTheSameInstance()
			throws SemanticException {
		// the program point is not used by the scoping operations under test
		ProgramPoint pp = null;
		ScopeToken token = new ScopeToken(() -> SyntheticLocation.INSTANCE);
		FixpointInfo info = new FixpointInfo(Map.of("plain", set("x")));
		assertSame(info, info.pushScope(token, pp));
		assertSame(info, info.popScope(token, pp));
		assertSame(FixpointInfo.TOP, FixpointInfo.TOP.pushScope(token, pp));
		assertSame(FixpointInfo.BOTTOM, FixpointInfo.BOTTOM.popScope(token, pp));
	}

	@Test
	public void getOnMissingKeyReturnsNull() {
		FixpointInfo info = new FixpointInfo(Map.of("a", set("x")));
		assertNull(info.get("missing"));
	}

	@Test
	public void getOnTopOrBottomReturnsNullRegardlessOfKey() {
		assertNull(FixpointInfo.TOP.get("anything"));
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
	public void topIsTheAbsorbingElementOfLub()
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
