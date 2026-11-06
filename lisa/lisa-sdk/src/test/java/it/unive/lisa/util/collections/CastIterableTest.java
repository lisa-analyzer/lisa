package it.unive.lisa.util.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CastIterableTest {

	@Test
	public void castsEachElementToTheGivenType() {
		List<Number> numbers = List.of(1, 2, 3);
		CastIterable<Number, Integer> casted = new CastIterable<>(numbers, Integer.class);

		List<Integer> collected = new ArrayList<>();
		for (Integer i : casted)
			collected.add(i);
		assertEquals(List.of(1, 2, 3), collected);
	}

	@Test
	public void mismatchedTypeThrowsClassCastExceptionOnNext() {
		List<Number> numbers = List.of(1, 2.5);
		CastIterable<Number, Integer> casted = new CastIterable<>(numbers, Integer.class);

		Iterator<Integer> it = casted.iterator();
		assertEquals(1, it.next());
		assertThrows(ClassCastException.class, it::next);
	}

	@Test
	public void removeDelegatesToWrappedIterator() {
		List<Number> numbers = new ArrayList<>(List.of(1, 2, 3));
		CastIterable<Number, Integer> casted = new CastIterable<>(numbers, Integer.class);

		Iterator<Integer> it = casted.iterator();
		it.next();
		it.remove();
		assertEquals(List.of(2, 3), numbers);
	}

}
