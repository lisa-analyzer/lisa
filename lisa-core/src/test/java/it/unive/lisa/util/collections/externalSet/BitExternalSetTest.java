package it.unive.lisa.util.collections.externalSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class BitExternalSetTest {

	private static final int ADD_LIMIT = 10000;
	private static final int REM_LIMIT = 50;

	private static final Random random = new Random();

	private static void populateSingle(Set<String>[] sets) {
		for (int limit = ADD_LIMIT + random.nextInt(ADD_LIMIT); limit >= 0; limit--) {
			String str = UUID.randomUUID().toString();
			for (Set<String> set : sets)
				set.add(str);
		}
	}

	private static void removeSingle(Set<String>[] sets) {
		List<String> copy = new ArrayList<>(sets[0]);
		Set<String> toRemove = new HashSet<>();
		for (int limit = REM_LIMIT + random.nextInt(REM_LIMIT); limit >= 0; limit--) {
			String str = copy.get(random.nextInt(copy.size()));
			if (!toRemove.contains(str))
				toRemove.add(str);
			else
				limit++;
		}

		for (String str : toRemove)
			for (Set<String> set : sets)
				set.remove(str);
	}

	private static void populateDouble(Set<String>[] sets1, Set<String>[] sets2) {
		for (int limit = ADD_LIMIT + random.nextInt(ADD_LIMIT); limit >= 0; limit--) {
			String str = UUID.randomUUID().toString();
			if (random.nextBoolean())
				for (Set<String> set : sets1)
					set.add(str);
			else
				for (Set<String> set : sets2)
					set.add(str);
		}
	}

	private static void populateTriple(Set<String>[] sets1, Set<String>[] sets2, Set<String>[] sets3) {
		for (int limit = ADD_LIMIT + random.nextInt(ADD_LIMIT); limit >= 0; limit--) {
			String str = UUID.randomUUID().toString();
			int flag = random.nextInt(3);
			if (flag == 0)
				for (Set<String> set : sets1)
					set.add(str);
			else if (flag == 1)
				for (Set<String> set : sets2)
					set.add(str);
			else
				for (Set<String> set : sets3)
					set.add(str);
		}
	}

	private static void verify(BiFunction<Set<String>, Set<String>, Boolean> test,
			Pair<Set<String>, Set<String>>... sets) {
		for (Pair<Set<String>, Set<String>> pair : sets)
			assertTrue("The pair of sets are different", test.apply(pair.getLeft(), pair.getRight()));
	}

	@Test
	public void testSingleSetAdd() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset = cache.mkEmptySet();
		Set<String> set = new HashSet<>();
		populateSingle(new Set[] { set, eset });

		verify(Set::equals, Pair.of(set, eset));
		verify(Set::containsAll, Pair.of(set, eset));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set, eset));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set, eset));
		verify((s, es) -> s.size() == es.size(), Pair.of(set, eset));
	}

	@Test
	public void testDoubleSetAdd() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		Set<String> set1 = new HashSet<>();
		Set<String> set2 = new HashSet<>();
		populateDouble(new Set[] { set1, eset1 }, new Set[] { set2, eset2 });
		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2));
	}

	@Test
	public void testTripleSetAdd() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		ExternalSet<String> eset3 = cache.mkEmptySet();
		Set<String> set1 = new HashSet<>();
		Set<String> set2 = new HashSet<>();
		Set<String> set3 = new HashSet<>();
		populateTriple(new Set[] { set1, eset1 }, new Set[] { set2, eset2 }, new Set[] { set3, eset3 });
		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
	}

	@Test
	public void testSingleSetAddAll() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset = cache.mkEmptySet();
		Set<String> set = new HashSet<>();
		populateSingle(new Set[] { set });
		eset.addAll(set);
		verify(Set::equals, Pair.of(set, eset));
		verify(Set::containsAll, Pair.of(set, eset));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set, eset));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set, eset));
		verify((s, es) -> s.size() == es.size(), Pair.of(set, eset));
	}

	@Test
	public void testDoubleSetAddAll() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		Set<String> set1 = new HashSet<>();
		Set<String> set2 = new HashSet<>();
		populateDouble(new Set[] { set1 }, new Set[] { set2 });
		eset1.addAll(set1);
		eset2.addAll(set2);
		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2));
	}

	@Test
	public void testTripleSetAddAll() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		ExternalSet<String> eset3 = cache.mkEmptySet();
		Set<String> set1 = new HashSet<>();
		Set<String> set2 = new HashSet<>();
		Set<String> set3 = new HashSet<>();
		populateTriple(new Set[] { set1 }, new Set[] { set2 }, new Set[] { set3 });
		eset1.addAll(set1);
		eset2.addAll(set2);
		eset3.addAll(set3);
		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
	}

	@Test
	public void testSingleSetAddAllExternals() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset = cache.mkEmptySet();
		ExternalSet<String> set = cache.mkEmptySet();
		populateSingle(new Set[] { set });
		eset.addAll(set);
		verify(Set::equals, Pair.of(set, eset));
		verify(Set::containsAll, Pair.of(set, eset));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set, eset));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set, eset));
		verify((s, es) -> s.size() == es.size(), Pair.of(set, eset));
	}

	@Test
	public void testDoubleSetAddAllExternals() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		ExternalSet<String> set1 = cache.mkEmptySet();
		ExternalSet<String> set2 = cache.mkEmptySet();
		populateDouble(new Set[] { set1 }, new Set[] { set2 });
		eset1.addAll(set1);
		eset2.addAll(set2);
		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2));
	}

	@Test
	public void testTripleSetAddAllExternals() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		ExternalSet<String> eset3 = cache.mkEmptySet();
		ExternalSet<String> set1 = cache.mkEmptySet();
		ExternalSet<String> set2 = cache.mkEmptySet();
		ExternalSet<String> set3 = cache.mkEmptySet();
		populateTriple(new Set[] { set1 }, new Set[] { set2 }, new Set[] { set3 });
		eset1.addAll(set1);
		eset2.addAll(set2);
		eset3.addAll(set3);
		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
	}

	@Test
	public void testSingleSetRemove() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset = cache.mkEmptySet();
		Set<String> set = new HashSet<>();
		populateSingle(new Set[] { set, eset });
		removeSingle(new Set[] { set, eset });

		verify(Set::equals, Pair.of(set, eset));
		verify(Set::containsAll, Pair.of(set, eset));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set, eset));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set, eset));
		verify((s, es) -> s.size() == es.size(), Pair.of(set, eset));
	}

	@Test
	public void testDoubleSetRemove() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		Set<String> set1 = new HashSet<>();
		Set<String> set2 = new HashSet<>();
		populateDouble(new Set[] { set1, eset1 }, new Set[] { set2, eset2 });
		removeSingle(new Set[] { set1, eset1 });
		removeSingle(new Set[] { set2, eset2 });

		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2));
	}

	@Test
	public void testTripleSetRemove() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		ExternalSet<String> eset3 = cache.mkEmptySet();
		Set<String> set1 = new HashSet<>();
		Set<String> set2 = new HashSet<>();
		Set<String> set3 = new HashSet<>();
		populateTriple(new Set[] { set1, eset1 }, new Set[] { set2, eset2 }, new Set[] { set3, eset3 });
		removeSingle(new Set[] { set1, eset1 });
		removeSingle(new Set[] { set2, eset2 });
		removeSingle(new Set[] { set3, eset3 });

		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2),
				Pair.of(set3, eset3));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2), Pair.of(set3, eset3));
	}

	@Test
	public void testSetOperations() {
		ExternalSetCache<String> cache = new ExternalSetCache<>();
		ExternalSet<String> eset1 = cache.mkEmptySet();
		ExternalSet<String> eset2 = cache.mkEmptySet();
		Set<String> set1 = new HashSet<>();
		Set<String> set2 = new HashSet<>();
		populateDouble(new Set[] { set1, eset1 }, new Set[] { set2, eset2 });

		verify(Set::equals, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify(Set::containsAll, Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.stream().allMatch(es::contains), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.isEmpty() == es.isEmpty(), Pair.of(set1, eset1), Pair.of(set2, eset2));
		verify((s, es) -> s.size() == es.size(), Pair.of(set1, eset1), Pair.of(set2, eset2));

		Set<String> tmp = new HashSet<>(set1);
		tmp.addAll(set2);
		verify(Set::equals, Pair.of(tmp, eset1.union(eset2)));

		tmp = new HashSet<>(set1);
		tmp.retainAll(set2);
		verify(Set::equals, Pair.of(tmp, eset1.intersection(eset2)));
		assertEquals(tmp.isEmpty(), !eset1.intersects(eset2));

		tmp = new HashSet<>(set1);
		tmp.removeAll(set2);
		verify(Set::equals, Pair.of(tmp, eset1.difference(eset2)));
	}
}
