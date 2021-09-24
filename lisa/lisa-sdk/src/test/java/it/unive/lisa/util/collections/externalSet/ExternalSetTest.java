package it.unive.lisa.util.collections.externalSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

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
}
