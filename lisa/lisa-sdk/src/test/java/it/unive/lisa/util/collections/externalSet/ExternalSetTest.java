package it.unive.lisa.util.collections.externalSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ExternalSetTest {

	@Test
	public void testMkEmpty() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		ExternalSet<Object> emptySet = cache.mkEmptySet();
		assertEquals(0, emptySet.size());
		assertTrue(emptySet.isEmpty());
	}

	@Test
	public void testMkFromSet() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		Set<Object> set = new HashSet<Object>();
		set.add("foo");
		set.add(new Object());
		ExternalSet<Object> eset = cache.mkSet(set);

		assertEquals(2, eset.size());
		assertTrue(eset.contains("foo"));
		assertFalse(eset.contains(new Object()));
	}

	@Test
	public void testMkFromExternalSet() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		ExternalSet<Object> eset = cache.mkEmptySet();
		eset.add("foo");
		eset.add(new Object());
		ExternalSet<Object> copy = cache.mkSet(eset);

		assertEquals(2, copy.size());
		assertTrue(copy.contains("foo"));
		assertFalse(copy.contains(new Object()));
	}

	@Test
	public void testCopy() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		ExternalSet<Object> eset = cache.mkEmptySet();
		eset.add("foo");
		eset.add(new Object());
		ExternalSet<Object> copy = eset.copy();

		assertEquals(2, copy.size());
		assertTrue(copy.contains("foo"));
		assertFalse(copy.contains(new Object()));
	}

	@Test
	public void testMkSingleton() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		ExternalSet<Object> singleton = cache.mkSingletonSet("foo");

		assertEquals(1, singleton.size());
		assertTrue(singleton.contains("foo"));
	}

	@Test
	public void testMkUniversal() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		ExternalSet<Object> universal = cache.mkUniversalSet();
		ExternalSet<Object> eset = cache.mkEmptySet();
		eset.add("foo");
		eset.add(new Object());

		assertEquals(2, universal.size());
		assertTrue(universal.contains("foo"));
		assertFalse(universal.contains(new Object()));
	}

	@Test
	public void testUniversalStaysUpToDateWithTheCache() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> universal = cache.mkUniversalSet();
		assertTrue(universal.isEmpty());

		cache.mkSingletonSet("a");
		assertEquals(1, universal.size());
		assertTrue(universal.contains("a"));

		cache.mkSingletonSet("b");
		assertEquals(2, universal.size());
		assertTrue(universal.contains("b"));
	}

	@Test
	public void testUniversalIsUnmodifiable() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> universal = cache.mkUniversalSet();

		assertThrows(UnsupportedOperationException.class, () -> universal.add("a"));
		assertThrows(UnsupportedOperationException.class, () -> universal.remove("a"));
		assertThrows(UnsupportedOperationException.class, () -> universal.addAll(List.of("a")));
		assertThrows(UnsupportedOperationException.class, () -> universal.retainAll(List.of("a")));
		assertThrows(UnsupportedOperationException.class, () -> universal.removeAll(List.of("a")));
		assertThrows(UnsupportedOperationException.class, universal::clear);
	}

	// regression test: the exception message must actually mention the
	// operation that failed, rather than always saying "remove" regardless
	// of which mutator was invoked
	@Test
	public void testUniversalUnsupportedMessageMentionsTheActualOperation() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> universal = cache.mkUniversalSet();

		UnsupportedOperationException addEx = assertThrows(
				UnsupportedOperationException.class,
				() -> universal.add("a"));
		assertTrue(addEx.getMessage().contains("add"), "add() message did not mention 'add': " + addEx.getMessage());

		UnsupportedOperationException removeEx = assertThrows(
				UnsupportedOperationException.class,
				() -> universal.remove("a"));
		assertTrue(
				removeEx.getMessage().contains("remove"),
				"remove() message did not mention 'remove': " + removeEx.getMessage());
	}

	@Test
	public void testUniversalCopyIsASnapshotBitExternalSet() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> universal = cache.mkUniversalSet();
		cache.mkSingletonSet("a");

		ExternalSet<String> copy = universal.copy();
		assertTrue(copy instanceof BitExternalSet);
		assertEquals(1, copy.size());
		assertTrue(copy.contains("a"));

		// the copy is a snapshot: further growth of the cache must not affect
		// it
		cache.mkSingletonSet("b");
		assertEquals(1, copy.size());
		assertEquals(2, universal.size());
	}

}
