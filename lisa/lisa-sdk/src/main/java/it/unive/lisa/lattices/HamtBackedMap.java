package it.unive.lisa.lattices;

import io.vavr.Tuple2;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link java.util.Map} view over a persistent (HAMT-backed) Vavr
 * {@link io.vavr.collection.Map}. Mutations swap the internal reference to
 * point at a new persistent map; structural sharing means most updates copy
 * only a single root-to-leaf path (~5 small arrays) rather than the entire
 * key/value table.
 * <p>
 * Designed to be used as the backing map of {@link FunctionalLattice}:
 * {@code mkNewFunction(otherHamt, false)} can return a fresh
 * {@code HamtBackedMap} that shares the entire backing with {@code other} in
 * O(1) instead of copying via {@code new HashMap<>(other)} in O(n).
 * <p>
 * Single-threaded use only. The underlying Vavr map is immutable, but the
 * reference swap on {@link #put}/{@link #remove} is unsynchronised; callers
 * must not share an instance across threads while mutating. (Per-instance
 * usage in LiSA's analysis pipeline is single-threaded.)
 */
public final class HamtBackedMap<K, V> extends AbstractMap<K, V> {

	private io.vavr.collection.Map<K, V> backing;

	/**
	 * Creates an empty HAMT-backed map.
	 */
	public HamtBackedMap() {
		this.backing = io.vavr.collection.HashMap.empty();
	}

	/**
	 * Creates a HAMT-backed map that shares the given persistent map as its
	 * backing. O(1).
	 *
	 * @param initial the initial persistent map
	 */
	public HamtBackedMap(
			io.vavr.collection.Map<K, V> initial) {
		this.backing = initial == null ? io.vavr.collection.HashMap.empty() : initial;
	}

	/**
	 * Creates a HAMT-backed map by copying the given {@link java.util.Map}.
	 * Used when bridging from legacy {@code HashMap}-backed code; the copy
	 * is O(n) and happens once at the boundary.
	 *
	 * @param other the source map to copy
	 */
	public HamtBackedMap(
			Map<? extends K, ? extends V> other) {
		if (other instanceof HamtBackedMap) {
			@SuppressWarnings("unchecked")
			io.vavr.collection.Map<K, V> shared = ((HamtBackedMap<K, V>) other).backing;
			this.backing = shared;
		} else {
			io.vavr.collection.Map<K, V> b = io.vavr.collection.HashMap.empty();
			for (Map.Entry<? extends K, ? extends V> e : other.entrySet())
				b = b.put(e.getKey(), e.getValue());
			this.backing = b;
		}
	}

	/**
	 * Yields the underlying persistent map. Intended for HAMT-aware fast
	 * paths in {@link FunctionalLattice} (e.g. incremental lub); callers
	 * should not mutate the returned reference, only construct new
	 * persistent maps from it.
	 *
	 * @return the underlying persistent map
	 */
	public io.vavr.collection.Map<K, V> backing() {
		return backing;
	}

	/**
	 * Replaces the underlying persistent map. The new value must agree with
	 * the existing entries that the caller relied on. Intended for the
	 * incremental lub fast path.
	 *
	 * @param newBacking the new persistent map
	 */
	public void setBacking(
			io.vavr.collection.Map<K, V> newBacking) {
		this.backing = newBacking == null ? io.vavr.collection.HashMap.empty() : newBacking;
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Override
	public boolean containsKey(
			Object key) {
		try {
			@SuppressWarnings("unchecked")
			K k = (K) key;
			return backing.containsKey(k);
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public V get(
			Object key) {
		try {
			@SuppressWarnings("unchecked")
			K k = (K) key;
			return backing.get(k).getOrNull();
		} catch (ClassCastException e) {
			return null;
		}
	}

	@Override
	public V put(
			K key,
			V value) {
		V prev = backing.get(key).getOrNull();
		backing = backing.put(key, value);
		return prev;
	}

	@Override
	public V remove(
			Object key) {
		try {
			@SuppressWarnings("unchecked")
			K k = (K) key;
			V prev = backing.get(k).getOrNull();
			backing = backing.remove(k);
			return prev;
		} catch (ClassCastException e) {
			return null;
		}
	}

	@Override
	public void clear() {
		backing = io.vavr.collection.HashMap.empty();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		// LinkedHashSet so iteration order is deterministic for the same
		// backing — important when callers serialise or compare-by-string.
		Set<Entry<K, V>> set = new LinkedHashSet<>(backing.size());
		for (Tuple2<K, V> t : backing)
			set.add(new SimpleImmutableEntry<>(t._1, t._2));
		return set;
	}

	@Override
	public boolean equals(
			Object o) {
		if (this == o)
			return true;
		if (o instanceof HamtBackedMap)
			return backing.equals(((HamtBackedMap<?, ?>) o).backing);
		// Fall back to Map equality contract: same size, same entries.
		if (!(o instanceof Map))
			return false;
		Map<?, ?> other = (Map<?, ?>) o;
		if (other.size() != backing.size())
			return false;
		for (Tuple2<K, V> t : backing)
			if (!Objects.equals(other.get(t._1), t._2))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		// Match java.util.Map's contract: sum of entry hashCodes.
		// Vavr's hashCode is already structural; this delegates to it,
		// which is consistent with Map.entrySet().stream().mapToInt(...).sum().
		return backing.hashCode();
	}
}
