package it.unive.lisa.util.datastructures.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

public class PatriciaTrieMapTest {

	private static final class FixedHashKey {
		private final String label;
		private final int forcedHash;

		FixedHashKey(
				String label,
				int forcedHash) {
			this.label = label;
			this.forcedHash = forcedHash;
		}

		@Override
		public int hashCode() {
			return forcedHash;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (!(obj instanceof FixedHashKey))
				return false;
			return Objects.equals(label, ((FixedHashKey) obj).label);
		}

		@Override
		public String toString() {
			return label + "@h" + forcedHash;
		}
	}

	@SuppressWarnings("unchecked")
	private static <K, V> PatriciaTrieMap<K, V> trieOf(
			Object... pairs) {
		PatriciaTrieMap<K, V> map = PatriciaTrieMap.empty();
		for (int i = 0; i < pairs.length; i += 2) {
			K k = (K) pairs[i];
			V v = (V) pairs[i + 1];
			map = map.put(k, v);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	private static <K, V> HashMap<K, V> hashMapOf(
			Object... pairs) {
		HashMap<K, V> map = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			K k = (K) pairs[i];
			V v = (V) pairs[i + 1];
			map.put(k, v);
		}
		return map;
	}

	private static <K, V> void sameContent(
			PatriciaTrieMap<K, V> trie,
			HashMap<K, V> reference) {
		assertEquals(reference.size(), trie.size(), "size mismatch");
		assertEquals(reference.isEmpty(), trie.isEmpty(), "isEmpty mismatch");
		assertEquals(reference.keySet(), trie.keySet(), "keySet mismatch");
		assertEquals(new HashSet<>(reference.entrySet()), trie.entrySet(), "entrySet mismatch");
		for (K key : reference.keySet()) {
			assertTrue(trie.containsKey(key), "missing key: " + key);
			assertEquals(reference.get(key), trie.get(key), "value mismatch for key: " + key);
		}
	}

	@Test
	public void emptyMapIsEmpty() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.empty();
		assertTrue(trie.isEmpty());
		assertEquals(0, trie.size());
		assertNull(trie.get("anything"));
		assertFalse(trie.containsKey("anything"));
		assertFalse(trie.iterator().hasNext());
		assertTrue(trie.keySet().isEmpty());
		assertTrue(trie.values().isEmpty());
		assertTrue(trie.entrySet().isEmpty());
	}

	@Test
	public void emptyMapEqualsBehavesAsHashMap() {
		sameContent(PatriciaTrieMap.empty(), new HashMap<>());
	}

	@Test
	public void singletonContainsOneEntry() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.singleton("a", 1);
		HashMap<String, Integer> ref = hashMapOf("a", 1);
		sameContent(trie, ref);
	}

	@Test
	public void putSingleKeyIsRetrievable() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.<String, Integer>empty().put("hello", 42);
		assertEquals(42, trie.get("hello"));
		assertEquals(1, trie.size());
	}

	@Test
	public void putMultipleDistinctKeys() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2, "c", 3);
		HashMap<String, Integer> ref = hashMapOf("a", 1, "b", 2, "c", 3);
		sameContent(trie, ref);
	}

	@Test
	public void putReplacesExistingKey() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.<String, Integer>empty()
				.put("x", 10)
				.put("x", 99);
		assertEquals(99, trie.get("x"));
		assertEquals(1, trie.size());
	}

	@Test
	public void putManyKeysMatchesHashMap() {
		PatriciaTrieMap<Integer, String> trie = PatriciaTrieMap.empty();
		HashMap<Integer, String> ref = new HashMap<>();
		for (int i = 0; i < 200; i++) {
			trie = trie.put(i, "v" + i);
			ref.put(i, "v" + i);
		}
		sameContent(trie, ref);
	}

	@Test
	public void putIsImmutable() {
		PatriciaTrieMap<String, Integer> original = PatriciaTrieMap.singleton("a", 1);
		PatriciaTrieMap<String, Integer> updated = original.put("b", 2);
		assertNotSame(original, updated);
		assertFalse(original.containsKey("b"), "original must not see new key");
		assertTrue(updated.containsKey("b"));
	}

	@Test
	public void getMissingKeyReturnsNull() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.singleton("a", 1);
		assertNull(trie.get("z"));
	}

	@Test
	public void containsKeyAbsent() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.singleton("a", 1);
		assertFalse(trie.containsKey("z"));
	}

	@Test
	public void containsKeyPresent() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.singleton("a", 1);
		assertTrue(trie.containsKey("a"));
	}

	@Test
	public void removeExistingKey() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2, "c", 3);
		PatriciaTrieMap<String, Integer> result = trie.remove("b");
		HashMap<String, Integer> ref = hashMapOf("a", 1, "c", 3);
		sameContent(result, ref);
	}

	@Test
	public void removeAbsentKeyReturnsThis() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.singleton("a", 1);
		assertSame(trie, trie.remove("z"));
	}

	@Test
	public void removeLastKeyProducesEmptyMap() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.singleton("a", 1).remove("a");
		assertTrue(trie.isEmpty());
		assertEquals(0, trie.size());
	}

	@Test
	public void removeIsImmutable() {
		PatriciaTrieMap<String, Integer> original = trieOf("a", 1, "b", 2);
		PatriciaTrieMap<String, Integer> shrunk = original.remove("a");
		assertNotSame(original, shrunk);
		assertTrue(original.containsKey("a"), "original must not lose key");
		assertFalse(shrunk.containsKey("a"));
	}

	@Test
	public void removeManyKeysMatchesHashMap() {
		PatriciaTrieMap<Integer, String> trie = PatriciaTrieMap.empty();
		HashMap<Integer, String> ref = new HashMap<>();
		for (int i = 0; i < 100; i++) {
			trie = trie.put(i, "v" + i);
			ref.put(i, "v" + i);
		}
		for (int i = 0; i < 100; i += 3) {
			trie = trie.remove(i);
			ref.remove(i);
		}
		sameContent(trie, ref);
	}

	@Test
	public void nullKeyIsSupported() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.<String, Integer>empty().put(null, 7);
		assertEquals(7, trie.get(null));
		assertTrue(trie.containsKey(null));
		sameContent(trie, hashMapOf(null, 7));
	}

	@Test
	public void nullValueIsSupported() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.<String, Integer>empty().put("k", null);
		assertNull(trie.get("k"));
		assertTrue(trie.containsKey("k"));
		sameContent(trie, hashMapOf("k", null));
	}

	@Test
	public void removeNullKey() {
		PatriciaTrieMap<String, Integer> trie = PatriciaTrieMap.<String, Integer>empty()
				.put(null, 7).put("a", 1);
		PatriciaTrieMap<String, Integer> result = trie.remove(null);
		assertFalse(result.containsKey(null));
		assertTrue(result.containsKey("a"));
	}

	@Test
	public void twoKeysWithSameHashAreStoredSeparately() {
		FixedHashKey k1 = new FixedHashKey("k1", 42);
		FixedHashKey k2 = new FixedHashKey("k2", 42); // same hash, different
														// key
		PatriciaTrieMap<FixedHashKey, String> trie = PatriciaTrieMap.<FixedHashKey, String>empty()
				.put(k1, "v1").put(k2, "v2");
		assertEquals("v1", trie.get(k1));
		assertEquals("v2", trie.get(k2));
		assertEquals(2, trie.size());
	}

	@Test
	public void manyCollisionsMatchHashMap() {
		// All keys share the same hash
		List<FixedHashKey> keys = new ArrayList<>();
		for (int i = 0; i < 10; i++)
			keys.add(new FixedHashKey("key" + i, 999));

		PatriciaTrieMap<FixedHashKey, Integer> trie = PatriciaTrieMap.empty();
		HashMap<FixedHashKey, Integer> ref = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			trie = trie.put(keys.get(i), i);
			ref.put(keys.get(i), i);
		}
		sameContent(trie, ref);
	}

	@Test
	public void collisionNodeRemoveReducesToLeaf() {
		FixedHashKey k1 = new FixedHashKey("k1", 5);
		FixedHashKey k2 = new FixedHashKey("k2", 5);
		PatriciaTrieMap<FixedHashKey, String> trie = PatriciaTrieMap.<FixedHashKey, String>empty()
				.put(k1, "a").put(k2, "b").remove(k1);
		assertFalse(trie.containsKey(k1));
		assertEquals("b", trie.get(k2));
		assertEquals(1, trie.size());
	}

	@Test
	public void collisionReplaceValue() {
		FixedHashKey k1 = new FixedHashKey("k1", 100);
		FixedHashKey k2 = new FixedHashKey("k2", 100);
		PatriciaTrieMap<FixedHashKey, Integer> trie = PatriciaTrieMap.<FixedHashKey, Integer>empty()
				.put(k1, 1).put(k2, 2).put(k1, 99);
		assertEquals(99, trie.get(k1));
		assertEquals(2, trie.get(k2));
		assertEquals(2, trie.size());
	}

	@Test
	public void iteratorCoversAllEntries() {
		PatriciaTrieMap<Integer, String> trie = PatriciaTrieMap.empty();
		HashMap<Integer, String> ref = new HashMap<>();
		for (int i = 0; i < 50; i++) {
			trie = trie.put(i, "x" + i);
			ref.put(i, "x" + i);
		}
		Set<Map.Entry<Integer, String>> iterated = new HashSet<>();
		for (Map.Entry<Integer, String> e : trie)
			iterated.add(e);
		assertEquals(ref.entrySet(), iterated);
	}

	@Test
	public void forEachCoversAllEntries() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2, "c", 3);
		HashMap<String, Integer> collected = new HashMap<>();
		trie.forEach(collected::put);
		assertEquals(hashMapOf("a", 1, "b", 2, "c", 3), collected);
	}

	@Test
	public void twoTriesWithSameEntriesAreEqual() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("x", 10, "y", 20);
		PatriciaTrieMap<String, Integer> t2 = trieOf("y", 20, "x", 10);
		assertEquals(t1, t2);
	}

	@Test
	public void triesWithDifferentEntriesAreNotEqual() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("x", 10);
		PatriciaTrieMap<String, Integer> t2 = trieOf("x", 99);
		assertFalse(t1.equals(t2));
	}

	@Test
	public void equalTriesHaveSameHashCode() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("a", 1, "b", 2);
		PatriciaTrieMap<String, Integer> t2 = trieOf("b", 2, "a", 1);
		assertEquals(t1.hashCode(), t2.hashCode());
	}

	@Test
	public void hashCodeMatchesMapContract() {
		// PatriciaTrieMap.hashCode() uses the same formula as java.util.Map:
		// sum of (key.hashCode() ^ value.hashCode())
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2, "c", 3);
		HashMap<String, Integer> ref = hashMapOf("a", 1, "b", 2, "c", 3);
		assertEquals(ref.hashCode(), trie.hashCode());
	}

	@Test
	public void unionDisjointMapsContainsBothEntries() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("a", 1, "b", 2);
		PatriciaTrieMap<String, Integer> t2 = trieOf("c", 3, "d", 4);
		PatriciaTrieMap<String, Integer> result = t1.union(t2, (
				a,
				b) -> a + b);
		sameContent(result, hashMapOf("a", 1, "b", 2, "c", 3, "d", 4));
	}

	@Test
	public void unionAppliesMergerForSharedKeys() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("a", 10, "b", 20);
		PatriciaTrieMap<String, Integer> t2 = trieOf("b", 5, "c", 30);
		PatriciaTrieMap<String, Integer> result = t1.union(t2, Integer::sum);
		// "b" is shared: 20 + 5 = 25
		sameContent(result, hashMapOf("a", 10, "b", 25, "c", 30));
	}

	@Test
	public void unionWithEmptyIsIdentity() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2);
		assertSame(trie, trie.union(PatriciaTrieMap.empty(), Integer::sum));
		assertEquals(trie, PatriciaTrieMap.<String, Integer>empty().union(trie, Integer::sum));
	}

	@Test
	public void unionWithSelfReturnsSame() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2);
		assertSame(trie, trie.union(trie, Integer::sum));
	}

	@Test
	public void unionMatchesHashMap() {
		PatriciaTrieMap<Integer, Integer> t1 = PatriciaTrieMap.empty();
		PatriciaTrieMap<Integer, Integer> t2 = PatriciaTrieMap.empty();
		HashMap<Integer, Integer> ref1 = new HashMap<>();
		HashMap<Integer, Integer> ref2 = new HashMap<>();
		for (int i = 0; i < 60; i++) {
			t1 = t1.put(i, i);
			ref1.put(i, i);
		}
		for (int i = 30; i < 90; i++) {
			t2 = t2.put(i, i * 10);
			ref2.put(i, i * 10);
		}
		// Merge: for overlapping keys (30..59), keep max
		PatriciaTrieMap<Integer, Integer> result = t1.union(t2, Math::max);
		HashMap<Integer, Integer> refResult = new HashMap<>(ref1);
		ref2.forEach((
				k,
				v) -> refResult.merge(k, v, Math::max));
		sameContent(result, refResult);
	}

	@Test
	public void unionWithCollisionsMatchesHashMap() {
		FixedHashKey k1 = new FixedHashKey("k1", 7);
		FixedHashKey k2 = new FixedHashKey("k2", 7); // collision
		FixedHashKey k3 = new FixedHashKey("k3", 7); // collision

		PatriciaTrieMap<FixedHashKey, Integer> t1 = PatriciaTrieMap.<FixedHashKey, Integer>empty()
				.put(k1, 1).put(k2, 2);
		PatriciaTrieMap<FixedHashKey, Integer> t2 = PatriciaTrieMap.<FixedHashKey, Integer>empty()
				.put(k2, 20).put(k3, 3);
		PatriciaTrieMap<FixedHashKey, Integer> result = t1.union(t2, Integer::sum);

		HashMap<FixedHashKey, Integer> ref = new HashMap<>();
		ref.put(k1, 1);
		ref.put(k2, 22); // 2 + 20
		ref.put(k3, 3);
		sameContent(result, ref);
	}

	@Test
	public void intersectionDisjointMapsIsEmpty() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("a", 1, "b", 2);
		PatriciaTrieMap<String, Integer> t2 = trieOf("c", 3, "d", 4);
		assertTrue(t1.intersection(t2, Integer::sum).isEmpty());
	}

	@Test
	public void intersectionKeepsOnlySharedKeys() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("a", 1, "b", 2, "c", 3);
		PatriciaTrieMap<String, Integer> t2 = trieOf("b", 10, "c", 20, "d", 30);
		PatriciaTrieMap<String, Integer> result = t1.intersection(t2, Integer::sum);
		sameContent(result, hashMapOf("b", 12, "c", 23));
	}

	@Test
	public void intersectionWithEmptyIsEmpty() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2);
		assertTrue(trie.intersection(PatriciaTrieMap.empty(), Integer::sum).isEmpty());
		assertTrue(PatriciaTrieMap.<String, Integer>empty().intersection(trie, Integer::sum).isEmpty());
	}

	@Test
	public void intersectionWithSelfReturnsSame() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2);
		assertSame(trie, trie.intersection(trie, Integer::sum));
	}

	@Test
	public void intersectionMatchesHashMap() {
		PatriciaTrieMap<Integer, Integer> t1 = PatriciaTrieMap.empty();
		PatriciaTrieMap<Integer, Integer> t2 = PatriciaTrieMap.empty();
		HashMap<Integer, Integer> ref1 = new HashMap<>();
		HashMap<Integer, Integer> ref2 = new HashMap<>();
		for (int i = 0; i < 80; i++) {
			t1 = t1.put(i, i);
			ref1.put(i, i);
		}
		for (int i = 40; i < 120; i++) {
			t2 = t2.put(i, i * 2);
			ref2.put(i, i * 2);
		}
		PatriciaTrieMap<Integer, Integer> result = t1.intersection(t2, Integer::sum);
		HashMap<Integer, Integer> refResult = new HashMap<>();
		ref1.forEach((
				k,
				v) -> {
			if (ref2.containsKey(k))
				refResult.put(k, v + ref2.get(k));
		});
		sameContent(result, refResult);
	}

	@Test
	public void intersectionWithCollisionsMatchesHashMap() {
		FixedHashKey k1 = new FixedHashKey("k1", 13);
		FixedHashKey k2 = new FixedHashKey("k2", 13); // collision
		FixedHashKey k3 = new FixedHashKey("k3", 13); // collision

		PatriciaTrieMap<FixedHashKey, Integer> t1 = PatriciaTrieMap.<FixedHashKey, Integer>empty()
				.put(k1, 10).put(k2, 20);
		PatriciaTrieMap<FixedHashKey, Integer> t2 = PatriciaTrieMap.<FixedHashKey, Integer>empty()
				.put(k2, 5).put(k3, 7);
		PatriciaTrieMap<FixedHashKey, Integer> result = t1.intersection(t2, Integer::sum);

		HashMap<FixedHashKey, Integer> ref = new HashMap<>();
		ref.put(k2, 25); // 20 + 5; k1 only in t1, k3 only in t2
		sameContent(result, ref);
	}

	@Test
	public void emptyIsSubmapOfAnything() {
		PatriciaTrieMap<String, Integer> empty = PatriciaTrieMap.empty();
		PatriciaTrieMap<String, Integer> nonempty = trieOf("a", 1);
		assertTrue(empty.isSubmapOf(nonempty, Objects::equals));
		assertTrue(empty.isSubmapOf(empty, Objects::equals));
	}

	@Test
	public void nonEmptyIsNotSubmapOfEmpty() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1);
		assertFalse(trie.isSubmapOf(PatriciaTrieMap.empty(), Objects::equals));
	}

	@Test
	public void selfIsSubmapOfSelf() {
		PatriciaTrieMap<String, Integer> trie = trieOf("a", 1, "b", 2);
		assertTrue(trie.isSubmapOf(trie, Objects::equals));
	}

	@Test
	public void strictSubsetIsSubmap() {
		PatriciaTrieMap<String, Integer> small = trieOf("a", 1, "b", 2);
		PatriciaTrieMap<String, Integer> large = trieOf("a", 1, "b", 2, "c", 3);
		assertTrue(small.isSubmapOf(large, Objects::equals));
		assertFalse(large.isSubmapOf(small, Objects::equals));
	}

	@Test
	public void isSubmapOfRespectValueOrder() {
		// Use integer ≤ as the value order
		PatriciaTrieMap<String, Integer> small = trieOf("a", 1, "b", 2);
		PatriciaTrieMap<String, Integer> large = trieOf("a", 5, "b", 5);
		assertTrue(small.isSubmapOf(large, (
				x,
				y) -> x <= y));
		assertFalse(large.isSubmapOf(small, (
				x,
				y) -> x <= y));
	}

	@Test
	public void isSubmapOfMissingKeyReturnsFalse() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("a", 1, "z", 9);
		PatriciaTrieMap<String, Integer> t2 = trieOf("a", 1, "b", 2);
		assertFalse(t1.isSubmapOf(t2, Objects::equals));
	}

	@Test
	public void isSubmapOfWithValueViolationReturnsFalse() {
		PatriciaTrieMap<String, Integer> t1 = trieOf("a", 10);
		PatriciaTrieMap<String, Integer> t2 = trieOf("a", 5);
		assertFalse(t1.isSubmapOf(t2, (
				x,
				y) -> x <= y));
	}

	@Test
	public void isSubmapOfWithCollisions() {
		FixedHashKey k1 = new FixedHashKey("k1", 77);
		FixedHashKey k2 = new FixedHashKey("k2", 77);
		PatriciaTrieMap<FixedHashKey, Integer> small = PatriciaTrieMap.<FixedHashKey, Integer>empty()
				.put(k1, 1);
		PatriciaTrieMap<FixedHashKey, Integer> large = PatriciaTrieMap.<FixedHashKey, Integer>empty()
				.put(k1, 1).put(k2, 2);
		assertTrue(small.isSubmapOf(large, Objects::equals));
		assertFalse(large.isSubmapOf(small, Objects::equals));
	}

	/**
	 * A variable whose strength flag does NOT affect equals/hashCode: two
	 * variables with the same name but different strengths are considered equal
	 * and hash identically.
	 */
	private static final class Variable {
		private final String name;
		private final boolean weak;

		Variable(
				String name,
				boolean weak) {
			this.name = name;
			this.weak = weak;
		}

		boolean isWeak() {
			return weak;
		}

		@Override
		public boolean equals(
				Object obj) {
			if (!(obj instanceof Variable))
				return false;
			return name.equals(((Variable) obj).name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name + (weak ? "(W)" : "(S)");
		}
	}

	private static Variable strong(
			String name) {
		return new Variable(name, false);
	}

	private static Variable weak(
			String name) {
		return new Variable(name, true);
	}

	// keyMerger: keep the weak key (lub on strength: STRONG ⊔ WEAK = WEAK)
	private static final BiFunction<Variable, Variable, Variable> KEEP_WEAK = (
			k1,
			k2) -> k1.isWeak() ? k1 : k2;

	// keyMerger: keep the strong key (glb on strength: STRONG ⊓ WEAK = STRONG)
	private static final BiFunction<Variable, Variable, Variable> KEEP_STRONG = (
			k1,
			k2) -> k1.isWeak() ? k2 : k1;

	// keyLeq: STRONG ≤ STRONG, STRONG ≤ WEAK, WEAK ≤ WEAK; NOT WEAK ≤ STRONG
	private static final BiPredicate<Variable, Variable> STRENGTH_LEQ = (
			k1,
			k2) -> !k1.isWeak() || k2.isWeak();

	@Test
	public void unionKeepsWeakKeyWhenStrongMeetsWeak() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(strong("x"), 10);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(weak("x"), 20);
		PatriciaTrieMap<Variable, Integer> result = t1.union(t2, KEEP_WEAK, Integer::sum);

		assertEquals(1, result.size());
		// The stored key must be the weak version
		Variable stored = result.keySet().iterator().next();
		assertTrue(stored.isWeak(), "union(strong, weak) must store the weak key");
		assertEquals(30, result.get(strong("x")));
	}

	@Test
	public void unionKeepsWeakKeyWhenWeakMeetsStrong() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(weak("x"), 10);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(strong("x"), 20);
		PatriciaTrieMap<Variable, Integer> result = t1.union(t2, KEEP_WEAK, Integer::sum);

		Variable stored = result.keySet().iterator().next();
		assertTrue(stored.isWeak(), "union(weak, strong) must store the weak key");
		assertEquals(30, result.get(weak("x")));
	}

	@Test
	public void unionKeepsStrongWhenBothStrong() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(strong("x"), 10);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(strong("x"), 20);
		PatriciaTrieMap<Variable, Integer> result = t1.union(t2, KEEP_WEAK, Integer::sum);

		Variable stored = result.keySet().iterator().next();
		assertFalse(stored.isWeak(), "union(strong, strong) must keep the strong key");
		assertEquals(30, result.get(strong("x")));
	}

	@Test
	public void unionKeepsWeakWhenBothWeak() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(weak("x"), 10);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(weak("x"), 20);
		PatriciaTrieMap<Variable, Integer> result = t1.union(t2, KEEP_WEAK, Integer::sum);

		Variable stored = result.keySet().iterator().next();
		assertTrue(stored.isWeak(), "union(weak, weak) must keep the weak key");
		assertEquals(30, result.get(weak("x")));
	}

	@Test
	public void unionDisjointKeysPreserveOriginalStrengths() {
		// Variables not present in both maps must be kept as-is
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(strong("a"), 1);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(weak("b"), 2);
		PatriciaTrieMap<Variable, Integer> result = t1.union(t2, KEEP_WEAK, Integer::sum);

		assertEquals(2, result.size());
		Variable ka = result.keySet().stream().filter(k -> k.name.equals("a")).findFirst().get();
		Variable kb = result.keySet().stream().filter(k -> k.name.equals("b")).findFirst().get();
		assertFalse(ka.isWeak(), "'a' was only in t1 as strong, must remain strong");
		assertTrue(kb.isWeak(), "'b' was only in t2 as weak, must remain weak");
	}

	@Test
	public void intersectionKeepsStrongKeyWhenStrongMeetsWeak() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(strong("x"), 10);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(weak("x"), 20);
		PatriciaTrieMap<Variable, Integer> result = t1.intersection(t2, KEEP_STRONG, Integer::sum);

		assertEquals(1, result.size());
		Variable stored = result.keySet().iterator().next();
		assertFalse(stored.isWeak(), "intersection(strong, weak) must store the strong key");
		assertEquals(30, result.get(strong("x")));
	}

	@Test
	public void intersectionKeepsStrongKeyWhenWeakMeetsStrong() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(weak("x"), 10);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(strong("x"), 20);
		PatriciaTrieMap<Variable, Integer> result = t1.intersection(t2, KEEP_STRONG, Integer::sum);

		Variable stored = result.keySet().iterator().next();
		assertFalse(stored.isWeak(), "intersection(weak, strong) must store the strong key");
		assertEquals(30, result.get(strong("x")));
	}

	@Test
	public void isSubmapOfStrongLeqWeak() {
		// STRONG ≤ WEAK: a strong entry is covered by a weak entry in other
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(strong("x"), 5);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(weak("x"), 10);
		assertTrue(t1.isSubmapOf(t2, STRENGTH_LEQ, (
				v1,
				v2) -> v1 <= v2));
	}

	@Test
	public void isSubmapOfWeakNotLeqStrong() {
		// WEAK ≤ STRONG must be false regardless of values
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(weak("x"), 1);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(strong("x"), 100);
		assertFalse(t1.isSubmapOf(t2, STRENGTH_LEQ, (
				v1,
				v2) -> v1 <= v2));
	}

	@Test
	public void isSubmapOfStrongLeqStrong() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(strong("x"), 5);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(strong("x"), 10);
		assertTrue(t1.isSubmapOf(t2, STRENGTH_LEQ, (
				v1,
				v2) -> v1 <= v2));
	}

	@Test
	public void isSubmapOfWeakLeqWeak() {
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(weak("x"), 5);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(weak("x"), 10);
		assertTrue(t1.isSubmapOf(t2, STRENGTH_LEQ, (
				v1,
				v2) -> v1 <= v2));
	}

	@Test
	public void isSubmapOfKeyLeqBlockedByValueOrder() {
		// Keys are compatible (STRONG ≤ WEAK) but values are not
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.singleton(strong("x"), 10);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.singleton(weak("x"), 5);
		assertFalse(t1.isSubmapOf(t2, STRENGTH_LEQ, (
				v1,
				v2) -> v1 <= v2));
	}

	@Test
	public void keyOperatorsWithMultipleVariables() {
		// Mixed-strength map: union of strong "a" and weak "b" against weak "a"
		// and strong "c"
		PatriciaTrieMap<Variable, Integer> t1 = PatriciaTrieMap.<Variable, Integer>empty()
				.put(strong("a"), 1).put(weak("b"), 2);
		PatriciaTrieMap<Variable, Integer> t2 = PatriciaTrieMap.<Variable, Integer>empty()
				.put(weak("a"), 10).put(strong("c"), 3);

		PatriciaTrieMap<Variable, Integer> result = t1.union(t2, KEEP_WEAK, Integer::sum);

		assertEquals(3, result.size());

		// "a": strong in t1, weak in t2 → weak in result, value 11
		Variable ka = result.keySet().stream().filter(k -> k.name.equals("a")).findFirst().get();
		assertTrue(ka.isWeak());
		assertEquals(11, result.get(strong("a")));

		// "b": only in t1 as weak → stays weak, value 2
		Variable kb = result.keySet().stream().filter(k -> k.name.equals("b")).findFirst().get();
		assertTrue(kb.isWeak());
		assertEquals(2, result.get(weak("b")));

		// "c": only in t2 as strong → stays strong, value 3
		Variable kc = result.keySet().stream().filter(k -> k.name.equals("c")).findFirst().get();
		assertFalse(kc.isWeak());
		assertEquals(3, result.get(strong("c")));
	}

	@Test
	public void stressTestPutGetRemoveMatchesHashMap() {
		PatriciaTrieMap<Integer, Integer> trie = PatriciaTrieMap.empty();
		HashMap<Integer, Integer> ref = new HashMap<>();

		// Interleaved puts and removes
		for (int i = 0; i < 500; i++) {
			trie = trie.put(i, i * i);
			ref.put(i, i * i);
		}
		for (int i = 0; i < 500; i += 7) {
			trie = trie.remove(i);
			ref.remove(i);
		}
		for (int i = 300; i < 600; i++) {
			trie = trie.put(i, -i);
			ref.put(i, -i);
		}
		sameContent(trie, ref);
	}

	@Test
	public void stressTestNegativeKeys() {
		// Exercises the sign-bit branch in the trie
		PatriciaTrieMap<Integer, Integer> trie = PatriciaTrieMap.empty();
		HashMap<Integer, Integer> ref = new HashMap<>();
		for (int i = -100; i <= 100; i++) {
			trie = trie.put(i, i);
			ref.put(i, i);
		}
		sameContent(trie, ref);
	}

	@Test
	public void stressTestExtremeHashValues() {
		FixedHashKey minHash = new FixedHashKey("min", Integer.MIN_VALUE);
		FixedHashKey maxHash = new FixedHashKey("max", Integer.MAX_VALUE);
		FixedHashKey zeroHash = new FixedHashKey("zero", 0);

		PatriciaTrieMap<FixedHashKey, String> trie = PatriciaTrieMap.<FixedHashKey, String>empty()
				.put(minHash, "min").put(maxHash, "max").put(zeroHash, "zero");
		assertEquals("min", trie.get(minHash));
		assertEquals("max", trie.get(maxHash));
		assertEquals("zero", trie.get(zeroHash));
		assertEquals(3, trie.size());
	}
}
