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
		assertEquals(emptySet.size(), 0);
		assertTrue(emptySet.isEmpty());
	}

	@Test
	public void testMkFromSet() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		Set<Object> set = new HashSet<Object>();
		set.add("foo");
		set.add(new Object());
		ExternalSet<Object> eset = cache.mkSet(set);

		assertEquals(eset.size(), 2);
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

		assertEquals(copy.size(), 2);
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

		assertEquals(copy.size(), 2);
		assertTrue(copy.contains("foo"));
		assertFalse(copy.contains(new Object()));
	}

	@Test
	public void testMkSingleton() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		ExternalSet<Object> singleton = cache.mkSingletonSet("foo");

		assertEquals(singleton.size(), 1);
		assertTrue(singleton.contains("foo"));
	}
	
	@Test 
	public void testMkUniversal() {
		ExternalSetCache<Object> cache = new ExternalSetCache<Object>();
		ExternalSet<Object> universal = cache.mkUniversalSet();
		ExternalSet<Object> eset = cache.mkEmptySet();
		eset.add("foo");
		eset.add(new Object());

		assertEquals(universal.size(), 2);
		assertTrue(universal.contains("foo"));
		assertFalse(universal.contains(new Object()));
	}
}
