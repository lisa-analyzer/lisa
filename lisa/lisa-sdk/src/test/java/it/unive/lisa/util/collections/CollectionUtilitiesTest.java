package it.unive.lisa.util.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.collections.CollectionUtilities.SortedSetCollector;
import it.unive.lisa.util.collections.CollectionUtilities.StringCollector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class CollectionUtilitiesTest {

	@Test
	public void nullSafeCompareDelegatesWhenBothNonNull() {
		assertEquals(0, CollectionUtilities.nullSafeCompare(true, "a", "a", Comparator.naturalOrder()));
		assertTrue(CollectionUtilities.nullSafeCompare(true, "a", "b", Comparator.naturalOrder()) < 0);
	}

	@Test
	public void nullSafeCompareBothNullIsZero() {
		assertEquals(0, CollectionUtilities.<String>nullSafeCompare(true, null, null, Comparator.naturalOrder()));
		assertEquals(0, CollectionUtilities.<String>nullSafeCompare(false, null, null, Comparator.naturalOrder()));
	}

	@Test
	public void nullSafeCompareNullFirstOrdersNullBeforeNonNull() {
		assertEquals(-1, CollectionUtilities.nullSafeCompare(true, null, "a", Comparator.naturalOrder()));
		assertEquals(1, CollectionUtilities.nullSafeCompare(true, "a", null, Comparator.naturalOrder()));
	}

	@Test
	public void nullSafeCompareNullLastOrdersNullAfterNonNull() {
		assertEquals(1, CollectionUtilities.nullSafeCompare(false, null, "a", Comparator.naturalOrder()));
		assertEquals(-1, CollectionUtilities.nullSafeCompare(false, "a", null, Comparator.naturalOrder()));
	}

	@Test
	public void equalsIsOrderInsensitiveAndUsesCustomEqualityTest() {
		List<String> first = Arrays.asList("a", "B", "c");
		List<String> second = Arrays.asList("C", "a", "b");
		assertTrue(CollectionUtilities.equals(first, second, String::equalsIgnoreCase));
	}

	@Test
	public void equalsRequiresBijectiveMatch() {
		// second has two copies of "a" (case-insensitively) but first only one:
		// there is no element left in first to match the extra "a" in second
		List<String> first = Arrays.asList("a", "b");
		List<String> second = Arrays.asList("a", "A");
		assertFalse(CollectionUtilities.equals(first, second, String::equalsIgnoreCase));
	}

	@Test
	public void equalsFalseWhenElementUnmatchedInEitherSide() {
		List<String> first = Arrays.asList("a", "b");
		List<String> second = Arrays.asList("a", "c");
		assertFalse(CollectionUtilities.equals(first, second, String::equalsIgnoreCase));
		assertFalse(CollectionUtilities.equals(second, first, String::equalsIgnoreCase));
	}

	@Test
	public void joinCombinesMatchedElementsAndKeepsUnmatchedAsIs() {
		List<String> first = Arrays.asList("a", "x");
		List<String> second = Arrays.asList("A", "y");
		List<String> result = new ArrayList<>();
		CollectionUtilities.join(
				first,
				second,
				result,
				String::equalsIgnoreCase,
				(
						l,
						r) -> l + r);

		assertEquals(3, result.size());
		assertTrue(result.contains("aA"));
		assertTrue(result.contains("x"));
		assertTrue(result.contains("y"));
	}

	@Test
	public void meetKeepsOnlyMatchedElements() {
		List<String> first = Arrays.asList("a", "x");
		List<String> second = Arrays.asList("A", "y");
		List<String> result = new ArrayList<>();
		CollectionUtilities.meet(
				first,
				second,
				result,
				String::equalsIgnoreCase,
				(
						l,
						r) -> l + r);

		assertEquals(1, result.size());
		assertEquals("aA", result.get(0));
	}

	@Test
	public void collectStoresGivenObjectsInAModifiableCollection() {
		Collection<String> collected = CollectionUtilities.collect("a", "b", "c");
		assertEquals(3, collected.size());
		assertTrue(collected.containsAll(Arrays.asList("a", "b", "c")));

		// must be modifiable
		collected.add("d");
		assertEquals(4, collected.size());
	}

	@Test
	public void collectOfNoArgumentsIsEmpty() {
		assertTrue(CollectionUtilities.collect().isEmpty());
	}

	@Test
	public void sortedSetCollectorSortsElements() {
		SortedSet<Integer> result = Arrays.asList(5, 3, 1, 4, 2).stream()
				.collect(new SortedSetCollector<>());
		assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(result));
	}

	@Test
	public void stringCollectorJoinsWithSeparatorSequentially() {
		String result = Arrays.asList("a", "b", "c").stream().collect(new StringCollector<>(", "));
		assertEquals("a, b, c", result);
	}

	@Test
	public void stringCollectorSingleElementHasNoSeparator() {
		assertEquals("a", Arrays.asList("a").stream().collect(new StringCollector<>(", ")));
	}

	@Test
	public void stringCollectorEmptyStreamYieldsEmptyString() {
		assertEquals("", new ArrayList<String>().stream().collect(new StringCollector<>(", ")));
	}

	// exercises the combiner, which is only invoked by the JDK for parallel
	// streams: a separator must appear between merged partial results, not
	// just within each partial one
	@Test
	public void stringCollectorInsertsSeparatorBetweenParallelPartials() {
		String result = IntStream.rangeClosed(1, 200)
				.parallel()
				.mapToObj(String::valueOf)
				.collect(new StringCollector<>(","));

		List<String> pieces = Arrays.asList(result.split(","));
		assertEquals(200, pieces.size());
		for (int i = 1; i <= 200; i++)
			assertTrue(pieces.contains(String.valueOf(i)), "Missing value " + i + " in " + result);
	}

}
