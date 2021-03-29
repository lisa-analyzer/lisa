package it.unive.lisa.util.workset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class WorksetTest {

	@SuppressWarnings("unchecked")
	private static <T> void random(WorkingSet<T> ws, boolean lifo, T... elements) {
		List<T> list = Arrays.asList(elements);
		Collections.shuffle(list);
		linear(ws, lifo, (T[]) list.toArray());
	}

	@SuppressWarnings("unchecked")
	private static <T> void linear(WorkingSet<T> ws, boolean lifo, T... elements) {
		assertTrue("The working set is not empty at the beginning", ws.isEmpty());

		try {
			ws.toString();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			fail("toString() raised an exception while empty");
		}

		for (int i = 0; i < elements.length; i++) {
			ws.push(elements[i]);
			assertEquals("Incorrect size while populating the working set", i + 1, ws.size());
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
				assertSame("pop() did not return the top-most element", elements[elements.length - i - 1], popped);
			else
				assertSame("pop() did not return the bottom-most element", elements[i], popped);

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
		linear(LIFOWorkingSet.mk(), true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		linear(LIFOWorkingSet.mk(), true, "a", "b", "c", "d", null);
		random(LIFOWorkingSet.mk(), true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(LIFOWorkingSet.mk(), true, "a", "b", "c", "d", null);

		// Concurrent version does not support null elements
		linear(ConcurrentLIFOWorkingSet.mk(), true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(ConcurrentLIFOWorkingSet.mk(), true, "a", "b", "c", "d", "e", "f", "g", "h", "i");
	}

	@Test
	public void FIFOsWsTest() {
		linear(FIFOWorkingSet.mk(), false, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		linear(FIFOWorkingSet.mk(), false, "a", "b", "c", "d", null);
		random(FIFOWorkingSet.mk(), false, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(FIFOWorkingSet.mk(), false, "a", "b", "c", "d", null);

		// Concurrent version does not support null elements
		linear(ConcurrentFIFOWorkingSet.mk(), false, "a", "b", "c", "d", "e", "f", "g", "h", "i");
		random(ConcurrentFIFOWorkingSet.mk(), false, "a", "b", "c", "d", "e", "f", "g", "h", "i");
	}
}
