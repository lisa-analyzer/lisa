package it.unive.lisa.util.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

public class IterableArrayTest {

	@Test
	public void sizeMatchesArrayLength() {
		assertEquals(3, new IterableArray<>(new String[] { "a", "b", "c" }).size());
		assertEquals(0, new IterableArray<>(new String[0]).size());
	}

	@Test
	public void iteratesInArrayOrder() {
		IterableArray<String> iterable = new IterableArray<>(new String[] { "a", "b", "c" });
		List<String> collected = new ArrayList<>();
		for (String s : iterable)
			collected.add(s);
		assertEquals(List.of("a", "b", "c"), collected);
	}

	@Test
	public void iteratorExhaustionThrows() {
		Iterator<String> it = new IterableArray<>(new String[] { "a" }).iterator();
		assertTrue(it.hasNext());
		assertEquals("a", it.next());
		assertFalse(it.hasNext());
		assertThrows(NoSuchElementException.class, it::next);
	}

	@Test
	public void iteratorRemoveIsUnsupported() {
		Iterator<String> it = new IterableArray<>(new String[] { "a" }).iterator();
		it.next();
		assertThrows(UnsupportedOperationException.class, it::remove);
	}

	@Test
	public void equalsAndHashCodeAreContentBased() {
		IterableArray<String> first = new IterableArray<>(new String[] { "a", "b" });
		IterableArray<String> second = new IterableArray<>(new String[] { "a", "b" });
		IterableArray<String> different = new IterableArray<>(new String[] { "a", "c" });

		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
		assertNotEquals(first, different);
	}

	@Test
	public void toStringMatchesArraysToString() {
		assertEquals("[a, b]", new IterableArray<>(new String[] { "a", "b" }).toString());
	}

}
