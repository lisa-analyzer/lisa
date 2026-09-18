package it.unive.lisa.util.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class CollectionsDiffBuilderTest {

	@Test
	public void sameContentBeforeComputeIsTrue() {
		CollectionsDiffBuilder<Integer> builder = new CollectionsDiffBuilder<>(
				Integer.class,
				List.of(1, 2),
				List.of(1, 2));
		assertTrue(builder.sameContent());
		assertTrue(builder.getCommons().isEmpty());
	}

	@Test
	public void identicalCollectionsAreFullyCommonAndSameContent() {
		CollectionsDiffBuilder<Integer> builder = new CollectionsDiffBuilder<>(
				Integer.class,
				Arrays.asList(3, 1, 2),
				Arrays.asList(2, 3, 1));
		builder.compute(Integer::compareTo);

		assertTrue(builder.sameContent());
		assertTrue(builder.getOnlyFirst().isEmpty());
		assertTrue(builder.getOnlySecond().isEmpty());
		assertEquals(3, builder.getCommons().size());
		for (Pair<Integer, Integer> pair : builder.getCommons())
			assertEquals(pair.getLeft(), pair.getRight());
	}

	@Test
	public void disjointCollectionsHaveNoCommons() {
		CollectionsDiffBuilder<Integer> builder = new CollectionsDiffBuilder<>(
				Integer.class,
				List.of(1, 2),
				List.of(3, 4));
		builder.compute(Integer::compareTo);

		assertFalse(builder.sameContent());
		assertTrue(builder.getCommons().isEmpty());
		assertEquals(List.of(1, 2), sorted(builder.getOnlyFirst()));
		assertEquals(List.of(3, 4), sorted(builder.getOnlySecond()));
	}

	@Test
	public void partialOverlapSplitsElementsCorrectly() {
		// 2 and 4 are common, 1 is only in first, 5 and 6 are only in second
		CollectionsDiffBuilder<Integer> builder = new CollectionsDiffBuilder<>(
				Integer.class,
				List.of(1, 2, 4),
				List.of(2, 4, 5, 6));
		builder.compute(Integer::compareTo);

		assertFalse(builder.sameContent());
		assertEquals(List.of(1), sorted(builder.getOnlyFirst()));
		assertEquals(List.of(5, 6), sorted(builder.getOnlySecond()));
		assertEquals(2, builder.getCommons().size());
	}

	@Test
	public void bothEmptyCollectionsAreSameContent() {
		CollectionsDiffBuilder<Integer> builder = new CollectionsDiffBuilder<>(
				Integer.class,
				Collections.emptyList(),
				Collections.emptyList());
		builder.compute(Integer::compareTo);

		assertTrue(builder.sameContent());
		assertTrue(builder.getCommons().isEmpty());
	}

	@Test
	public void recomputeResetsPreviousResults() {
		CollectionsDiffBuilder<Integer> builder = new CollectionsDiffBuilder<>(
				Integer.class,
				List.of(1),
				List.of(2));
		builder.compute(Integer::compareTo);
		assertFalse(builder.sameContent());

		// build again over collections that are now identical: previous
		// only-first/only-second results must not linger
		CollectionsDiffBuilder<Integer> reused = new CollectionsDiffBuilder<>(
				Integer.class,
				List.of(1),
				List.of(1));
		reused.compute(Integer::compareTo);
		assertTrue(reused.sameContent());
	}

	private static List<Integer> sorted(
			java.util.Collection<Integer> coll) {
		List<Integer> list = new java.util.ArrayList<>(coll);
		Collections.sort(list);
		return list;
	}

}
