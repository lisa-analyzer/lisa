package it.unive.lisa.util.collections.workset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class WorksetTest {

	@SuppressWarnings("unchecked")
	private static <T> void random(
			WorkingSet<T> ws,
			boolean lifo,
			boolean duplicates,
			T... elements) {
		List<T> list = Arrays.asList(elements);
		Collections.shuffle(list);
		linear(ws, lifo, duplicates, (T[]) list.toArray());
	}

	@SuppressWarnings("unchecked")
	private static <T> void linear(
			WorkingSet<T> ws,
			boolean lifo,
			boolean duplicates,
			T... elements) {
		assertTrue(ws.isEmpty(), "The working set is not empty at the beginning");

		try {
			ws.toString();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			fail("toString() raised an exception while empty");
		}

		List<T> processed = new LinkedList<>();
		for (int i = 0, skipped = 0; i < elements.length; i++) {
			if (processed.contains(elements[i]) && !duplicates) {
				skipped++;
				continue;
			}

			ws.push(elements[i]);
			processed.add(elements[i]);
			assertEquals(i - skipped + 1, ws.size(), "Incorrect size while populating the working set");
			if (lifo)
				assertSame(elements[i], ws.peek(), "peek() did not return the top-most element");
			else
				assertSame(elements[0], ws.peek(), "peek() did not return the bottom-most element");

			try {
				ws.toString();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				fail("toString() raised an exception while pushing elements");
			}
		}

		try {
			ws.toString();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			fail("toString() raised an exception at full size");
		}

		int i = 0;
		while (!ws.isEmpty()) {
			T peeked = ws.peek();
			T popped = ws.pop();

			assertSame(peeked, popped, "peek() did not return the bottom-most element");
			if (lifo)
				assertSame(
						processed.get(processed.size() - i - 1),
						popped,
						"pop() did not return the top-most element");
			else
				assertSame(processed.get(i), popped, "peek() did not return the bottom-most element");

			try {
				ws.toString();
			} catch (Exception e) {
				e.printStackTrace(System.err);
				fail("toString() raised an exception while popping elements");
			}

			i++;
		}
	}

	@Test
	public void LIFOsWsTest() {
		linear(new LIFOWorkingSet<>(), true, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		linear(new LIFOWorkingSet<>(), true, true, "a", "b", "c", "d", null);
		random(new LIFOWorkingSet<>(), true, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(new LIFOWorkingSet<>(), true, true, "a", "b", "c", "d", null);

		// Concurrent version does not support null elements
		linear(new ConcurrentLIFOWorkingSet<>(), true, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(new ConcurrentLIFOWorkingSet<>(), true, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
	}

	@Test
	public void FIFOsWsTest() {
		linear(new FIFOWorkingSet<>(), false, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		linear(new FIFOWorkingSet<>(), false, true, "a", "b", "c", "d", null);
		random(new FIFOWorkingSet<>(), false, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(new FIFOWorkingSet<>(), false, true, "a", "b", "c", "d", null);

		// Concurrent version does not support null elements
		linear(new ConcurrentFIFOWorkingSet<>(), false, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(new ConcurrentFIFOWorkingSet<>(), false, true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
	}

	interface Tester<T> {

		@SuppressWarnings("unchecked")
		void test(
				WorkingSet<T> ws,
				boolean lifo,
				T... elements);

	}

	@SafeVarargs
	private static <T> void unique(
			VisitOnceWorkingSet<T> ws,
			boolean lifo,
			Tester<T> tester,
			T... elements) {
		Set<T> set = new HashSet<>();
		List<T> list = new ArrayList<>();
		Set<T> elementsSet = new HashSet<>(Arrays.asList(elements));

		tester.test(ws, lifo, elements);

		ws.getSeen().forEach(s -> {
			set.add(s);
			list.add(s);
		});
		assertEquals(set.size(), list.size(), "peek() did not return the bottom-most element");
		assertTrue(
				elementsSet.containsAll(set) && set.containsAll(elementsSet),
				"Set of seen elements does not contain all elements");
	}

	@Test
	public void VisitOnceWsTest() {
		unique(
				new VisitOnceFIFOWorkingSet<>(),
				false,
				(
						ws,
						lifo,
						el) -> linear(ws, lifo, false, el),
				"a",
				"b",
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				"i");
		unique(
				new VisitOnceFIFOWorkingSet<>(),
				false,
				(
						ws,
						lifo,
						el) -> linear(ws, lifo, false, el),
				"a",
				null,
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				null,
				"i");
		unique(
				new VisitOnceFIFOWorkingSet<>(),
				false,
				(
						ws,
						lifo,
						el) -> random(ws, lifo, false, el),
				"a",
				"b",
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				"i");
		unique(
				new VisitOnceFIFOWorkingSet<>(),
				false,
				(
						ws,
						lifo,
						el) -> random(ws, lifo, false, el),
				"a",
				null,
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				null,
				"i");

		unique(
				new VisitOnceLIFOWorkingSet<>(),
				true,
				(
						ws,
						lifo,
						el) -> linear(ws, lifo, false, el),
				"a",
				"b",
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				"i");
		unique(
				new VisitOnceLIFOWorkingSet<>(),
				true,
				(
						ws,
						lifo,
						el) -> linear(ws, lifo, false, el),
				"a",
				null,
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				null,
				"i");
		unique(
				new VisitOnceLIFOWorkingSet<>(),
				true,
				(
						ws,
						lifo,
						el) -> random(ws, lifo, false, el),
				"a",
				"b",
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				"i");
		unique(
				new VisitOnceLIFOWorkingSet<>(),
				true,
				(
						ws,
						lifo,
						el) -> random(ws, lifo, false, el),
				"a",
				null,
				"c",
				"d",
				"d",
				"f",
				"a",
				"b",
				null,
				"i");
	}

}
