package it.unive.lisa.util.collections.workset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.Test;

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
		assertTrue("The working set is not empty at the beginning", ws.isEmpty());

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
			assertEquals("Incorrect size while populating the working set", i - skipped + 1, ws.size());
			if (lifo)
				assertSame("peek() did not return the top-most element", elements[i], ws.peek());
			else
				assertSame("peek() did not return the bottom-most element", elements[0], ws.peek());

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

			assertSame("pop() did not return the same element of peek()", peeked, popped);
			if (lifo)
				assertSame(
						"pop() did not return the top-most element",
						processed.get(processed.size() - i - 1),
						popped);
			else
				assertSame("pop() did not return the bottom-most element", processed.get(i), popped);

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
		assertEquals("Set of seen elements contains duplicates", set.size(), list.size());
		assertTrue(
				"Set of seen elements does not contain all elements",
				elementsSet.containsAll(set) && set.containsAll(elementsSet));
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
