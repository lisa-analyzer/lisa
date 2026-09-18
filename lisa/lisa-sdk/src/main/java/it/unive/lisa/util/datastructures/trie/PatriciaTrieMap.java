package it.unive.lisa.util.datastructures.trie;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * An immutable, persistent map backed by a <em>Patricia trie</em> (also known
 * as a PATRICIA tree or radix tree) indexed by the {@link Object#hashCode()} of
 * keys.
 * <p>
 * The implementation follows the algorithm described in Okasaki and Gill's
 * <em>Fast Mergeable Integer Maps</em> (ML Workshop, 1998) and is similar in
 * spirit to Haskell's {@code Data.IntMap}. Every key is reduced to its
 * {@code int} hash code; the trie branches on individual bits of these hash
 * codes from the most significant to the least significant. Actual key equality
 * ({@link Object#equals}) is used only at the leaf level so that hash
 * collisions (distinct keys sharing the same hash) are handled correctly via
 * dedicated {@code PatriciaTreeCollisionNode}s.
 * <h2>Structural sharing</h2>
 * <p>
 * Because all update operations ({@link #put}, {@link #remove}, {@link #union},
 * {@link #intersection}) return brand-new {@code PatriciaTrieMap} instances,
 * the untouched parts of the trie are <em>shared</em> between the old and the
 * new map. This structural sharing (a) reduces memory consumption and (b)
 * enables the reference-equality short-circuit in {@link #union},
 * {@link #intersection}, and {@link #isSubmapOf}: when two subtrees are
 * literally the same object, their result can be returned immediately without
 * further traversal.
 * <p>
 * Both null keys and null values are supported. A null key hashes to 0.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <K> the type of keys
 * @param <V> the type of values
 *
 * @see <a href="https://ittc.ku.edu/~andygill/papers/IntMap98.pdf">Okasaki
 *          &amp; Gill, Fast Mergeable Integer Maps, 1998</a>
 */
public final class PatriciaTrieMap<K, V>
		implements
		Iterable<Map.Entry<K, V>> {

	/**
	 * Returns true iff bit {@code m} is zero in {@code key}.
	 */
	private static boolean zeroBit(
			int key,
			int m) {
		return (key & m) == 0;
	}

	/**
	 * Returns true iff {@code key} belongs to the subtree rooted at a branch
	 * node with the given {@code prefix} and {@code branchBit}.
	 */
	private static boolean matchPrefix(
			int key,
			int prefix,
			int m) {
		return (key & ~(m | (m - 1))) == prefix;
	}

	/**
	 * Creates a new {@link PatriciaTrieBranch} that joins two subtrees whose
	 * key ranges are disjoint. The branching bit is determined by the highest
	 * bit at which {@code p1} and {@code p2} differ.
	 */
	private static <K, V> PatriciaTrieNode<K, V> join(
			int p1,
			PatriciaTrieNode<K, V> t1,
			int p2,
			PatriciaTrieNode<K, V> t2) {
		// Returns the highest bit position (as a power of two) at which p1 and
		// p2 differ. The result is always a positive power of two when p1 != p2
		int m = Integer.highestOneBit(p1 ^ p2);
		// (m | (m-1)) sets bit m and all bits below it; ~(m|(m-1)) keeps only
		// bits strictly above m. Works correctly for m = Integer.MIN_VALUE
		// because (MIN_VALUE | MAX_VALUE) == -1, and ~(-1) == 0.
		int prefix = p1 & ~(m | (m - 1));
		return zeroBit(p1, m)
				? new PatriciaTrieBranch<>(prefix, m, t1, t2)
				: new PatriciaTrieBranch<>(prefix, m, t2, t1);
	}

	/**
	 * Smart constructor for {@link PatriciaTrieBranch}: if one child is
	 * {@code null}, returns the other child directly (path compression).
	 * Otherwise constructs a new branch node.
	 */
	private static <K, V> PatriciaTrieNode<K, V> mkBranch(
			int prefix,
			int branchBit,
			PatriciaTrieNode<K, V> left,
			PatriciaTrieNode<K, V> right) {
		if (left == null)
			return right;
		if (right == null)
			return left;
		return new PatriciaTrieBranch<>(prefix, branchBit, left, right);
	}

	private static <K, V> PatriciaTrieNode<K, V> trieInsert(
			int hash,
			K key,
			V value,
			PatriciaTrieNode<K, V> node) {
		if (node == null)
			return new PatriciaTrieLeaf<>(hash, key, value);

		if (node instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> leaf = (PatriciaTrieLeaf<K, V>) node;
			if (hash == leaf.hash) {
				if (Objects.equals(key, leaf.key))
					return Objects.equals(value, leaf.value) ? node : new PatriciaTrieLeaf<>(hash, key, value);
				// Hash collision: promote to collision node
				return new PatriciaTrieCollision<>(hash,
						new Object[] { leaf.key, key },
						new Object[] { leaf.value, value });
			}
			return join(hash, new PatriciaTrieLeaf<>(hash, key, value), leaf.hash, leaf);
		}

		if (node instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col = (PatriciaTrieCollision<K, V>) node;
			if (hash == col.hash)
				return col.with(key, value);
			return join(hash, new PatriciaTrieLeaf<>(hash, key, value), col.hash, col);
		}

		PatriciaTrieBranch<K, V> branch = (PatriciaTrieBranch<K, V>) node;
		if (matchPrefix(hash, branch.prefix, branch.branchBit)) {
			if (zeroBit(hash, branch.branchBit)) {
				PatriciaTrieNode<K, V> newLeft = trieInsert(hash, key, value, branch.left);
				if (newLeft == branch.left)
					return node;
				return new PatriciaTrieBranch<>(branch.prefix, branch.branchBit, newLeft, branch.right);
			} else {
				PatriciaTrieNode<K, V> newRight = trieInsert(hash, key, value, branch.right);
				if (newRight == branch.right)
					return node;
				return new PatriciaTrieBranch<>(branch.prefix, branch.branchBit, branch.left, newRight);
			}
		}
		return join(hash, new PatriciaTrieLeaf<>(hash, key, value), branch.prefix, branch);
	}

	private static <K, V> V trieLookup(
			int hash,
			Object key,
			PatriciaTrieNode<K, V> node) {
		// Iterative traversal through branch nodes for efficiency
		while (node instanceof PatriciaTrieBranch) {
			PatriciaTrieBranch<K, V> branch = (PatriciaTrieBranch<K, V>) node;
			if (!matchPrefix(hash, branch.prefix, branch.branchBit))
				return null;
			node = zeroBit(hash, branch.branchBit) ? branch.left : branch.right;
		}
		if (node == null)
			return null;
		if (node instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> leaf = (PatriciaTrieLeaf<K, V>) node;
			return (hash == leaf.hash && Objects.equals(key, leaf.key)) ? leaf.value : null;
		}
		PatriciaTrieCollision<K, V> col = (PatriciaTrieCollision<K, V>) node;
		return (hash == col.hash) ? col.get(key) : null;
	}

	private static <K, V> boolean trieContainsKey(
			int hash,
			Object key,
			PatriciaTrieNode<K, V> node) {
		while (node instanceof PatriciaTrieBranch) {
			PatriciaTrieBranch<K, V> branch = (PatriciaTrieBranch<K, V>) node;
			if (!matchPrefix(hash, branch.prefix, branch.branchBit))
				return false;
			node = zeroBit(hash, branch.branchBit) ? branch.left : branch.right;
		}
		if (node == null)
			return false;
		if (node instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> leaf = (PatriciaTrieLeaf<K, V>) node;
			return hash == leaf.hash && Objects.equals(key, leaf.key);
		}
		PatriciaTrieCollision<K, V> col = (PatriciaTrieCollision<K, V>) node;
		return hash == col.hash && col.containsKey(key);
	}

	/**
	 * Returns the key-value pair whose key is equal (via
	 * {@link Objects#equals}) to {@code key} and whose hash matches
	 * {@code hash}, or {@code null} if no such entry exists. Unlike
	 * {@link #trieLookup}, this method also returns the stored key object,
	 * which may differ from {@code key} in object identity even when the two
	 * are equal.
	 */
	private static <K, V> Map.Entry<K, V> trieLookupEntry(
			int hash,
			Object key,
			PatriciaTrieNode<K, V> node) {
		while (node instanceof PatriciaTrieBranch) {
			PatriciaTrieBranch<K, V> branch = (PatriciaTrieBranch<K, V>) node;
			if (!matchPrefix(hash, branch.prefix, branch.branchBit))
				return null;
			node = zeroBit(hash, branch.branchBit) ? branch.left : branch.right;
		}
		if (node == null)
			return null;
		if (node instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> leaf = (PatriciaTrieLeaf<K, V>) node;
			if (hash == leaf.hash && Objects.equals(key, leaf.key))
				return new AbstractMap.SimpleImmutableEntry<>(leaf.key, leaf.value);
			return null;
		}
		PatriciaTrieCollision<K, V> col = (PatriciaTrieCollision<K, V>) node;
		if (hash != col.hash)
			return null;
		for (int i = 0; i < col.keys.length; i++) {
			if (Objects.equals(col.keys[i], key)) {
				@SuppressWarnings("unchecked")
				K k = (K) col.keys[i];
				@SuppressWarnings("unchecked")
				V v = (V) col.values[i];
				return new AbstractMap.SimpleImmutableEntry<>(k, v);
			}
		}
		return null;
	}

	private static <K, V> PatriciaTrieNode<K, V> trieRemove(
			int hash,
			Object key,
			PatriciaTrieNode<K, V> node) {
		if (node == null)
			return null;

		if (node instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> leaf = (PatriciaTrieLeaf<K, V>) node;
			return (hash == leaf.hash && Objects.equals(key, leaf.key)) ? null : node;
		}

		if (node instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col = (PatriciaTrieCollision<K, V>) node;
			if (hash != col.hash)
				return node;
			@SuppressWarnings("unchecked")
			PatriciaTrieNode<K, V> result = col.without((K) key);
			return result;
		}

		PatriciaTrieBranch<K, V> branch = (PatriciaTrieBranch<K, V>) node;
		if (!matchPrefix(hash, branch.prefix, branch.branchBit))
			return node;
		if (zeroBit(hash, branch.branchBit)) {
			PatriciaTrieNode<K, V> newLeft = trieRemove(hash, key, branch.left);
			if (newLeft == branch.left)
				return node;
			return mkBranch(branch.prefix, branch.branchBit, newLeft, branch.right);
		} else {
			PatriciaTrieNode<K, V> newRight = trieRemove(hash, key, branch.right);
			if (newRight == branch.right)
				return node;
			return mkBranch(branch.prefix, branch.branchBit, branch.left, newRight);
		}
	}

	/**
	 * Inserts {@code key -> value} into {@code node}, merging with any existing
	 * equal key via {@code keyMerger.apply(existingKey, incomingKey)} for the
	 * result key and {@code valueMerger.apply(existingValue, incomingValue)}
	 * for the result value.
	 * <p>
	 * The key merger is invoked only when an existing entry whose key is equal
	 * (via {@link Objects#equals}) to the incoming key is found. In all other
	 * cases the incoming key is used as-is.
	 */
	private static <K, V> PatriciaTrieNode<K, V> trieInsertWith(
			int hash,
			K key,
			V value,
			PatriciaTrieNode<K, V> node,
			BiFunction<? super K, ? super K, ? extends K> keyMerger,
			BiFunction<? super V, ? super V, ? extends V> valueMerger) {
		if (node == null)
			return new PatriciaTrieLeaf<>(hash, key, value);

		if (node instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> leaf = (PatriciaTrieLeaf<K, V>) node;
			if (hash == leaf.hash) {
				if (Objects.equals(key, leaf.key)) {
					K mergedKey = keyMerger.apply(leaf.key, key);
					V mergedVal = valueMerger.apply(leaf.value, value);
					if (mergedKey == leaf.key && Objects.equals(mergedVal, leaf.value))
						return node;
					return new PatriciaTrieLeaf<>(hash, mergedKey, mergedVal);
				}
				return new PatriciaTrieCollision<>(hash,
						new Object[] { leaf.key, key },
						new Object[] { leaf.value, value });
			}
			return join(hash, new PatriciaTrieLeaf<>(hash, key, value), leaf.hash, leaf);
		}

		if (node instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col = (PatriciaTrieCollision<K, V>) node;
			if (hash == col.hash) {
				for (int i = 0; i < col.keys.length; i++) {
					if (Objects.equals(col.keys[i], key)) {
						@SuppressWarnings("unchecked")
						K existingKey = (K) col.keys[i];
						@SuppressWarnings("unchecked")
						V existingVal = (V) col.values[i];
						K mergedKey = keyMerger.apply(existingKey, key);
						V mergedVal = valueMerger.apply(existingVal, value);
						if (mergedKey == existingKey && Objects.equals(mergedVal, existingVal))
							return node;
						Object[] nk = col.keys.clone();
						Object[] nv = col.values.clone();
						nk[i] = mergedKey;
						nv[i] = mergedVal;
						return new PatriciaTrieCollision<>(hash, nk, nv);
					}
				}
				return col.with(key, value);
			}
			return join(hash, new PatriciaTrieLeaf<>(hash, key, value), col.hash, col);
		}

		PatriciaTrieBranch<K, V> branch = (PatriciaTrieBranch<K, V>) node;
		if (matchPrefix(hash, branch.prefix, branch.branchBit)) {
			if (zeroBit(hash, branch.branchBit)) {
				PatriciaTrieNode<K, V> newLeft = trieInsertWith(hash, key, value, branch.left, keyMerger, valueMerger);
				if (newLeft == branch.left)
					return node;
				return new PatriciaTrieBranch<>(branch.prefix, branch.branchBit, newLeft, branch.right);
			} else {
				PatriciaTrieNode<K,
						V> newRight = trieInsertWith(hash, key, value, branch.right, keyMerger, valueMerger);
				if (newRight == branch.right)
					return node;
				return new PatriciaTrieBranch<>(branch.prefix, branch.branchBit, branch.left, newRight);
			}
		}
		return join(hash, new PatriciaTrieLeaf<>(hash, key, value), branch.prefix, branch);
	}

	/**
	 * Returns the union of {@code t1} and {@code t2}. For keys present in both,
	 * the result key is {@code keyMerger.apply(t1Key, t2Key)} and the result
	 * value is {@code valueMerger.apply(t1Value, t2Value)}.
	 * <p>
	 * If {@code t1 == t2} (reference equality), the node is returned as-is
	 * without recursion (structural-sharing short-circuit).
	 */
	private static <K, V> PatriciaTrieNode<K, V> trieUnion(
			PatriciaTrieNode<K, V> t1,
			PatriciaTrieNode<K, V> t2,
			BiFunction<? super K, ? super K, ? extends K> keyMerger,
			BiFunction<? super V, ? super V, ? extends V> valueMerger) {
		if (t1 == null)
			return t2;
		if (t2 == null)
			return t1;
		if (t1 == t2)
			return t1; // structural-sharing short-circuit

		if (t1 instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> l1 = (PatriciaTrieLeaf<K, V>) t1;
			// Insert l1 (from t1) into t2; in trieInsertWith "existing" is the
			// t2 key
			// so we flip both mergers to restore (t1, t2) argument order.
			return trieInsertWith(l1.hash, l1.key, l1.value, t2,
					(
							existingT2Key,
							incomingT1Key) -> keyMerger.apply(incomingT1Key, existingT2Key),
					(
							existingT2Val,
							incomingT1Val) -> valueMerger.apply(incomingT1Val, existingT2Val));
		}
		if (t2 instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> l2 = (PatriciaTrieLeaf<K, V>) t2;
			// Insert l2 (from t2) into t1; "existing" is the t1 key — no flip
			// needed.
			return trieInsertWith(l2.hash, l2.key, l2.value, t1, keyMerger, valueMerger);
		}

		if (t1 instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col1 = (PatriciaTrieCollision<K, V>) t1;
			PatriciaTrieNode<K, V> result = t2;
			for (int i = 0; i < col1.keys.length; i++) {
				@SuppressWarnings("unchecked")
				K k = (K) col1.keys[i];
				@SuppressWarnings("unchecked")
				V v = (V) col1.values[i];
				result = trieInsertWith(col1.hash, k, v, result,
						(
								existingT2Key,
								incomingT1Key) -> keyMerger.apply(incomingT1Key, existingT2Key),
						(
								existingT2Val,
								incomingT1Val) -> valueMerger.apply(incomingT1Val, existingT2Val));
			}
			return result;
		}
		if (t2 instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col2 = (PatriciaTrieCollision<K, V>) t2;
			PatriciaTrieNode<K, V> result = t1;
			for (int i = 0; i < col2.keys.length; i++) {
				@SuppressWarnings("unchecked")
				K k = (K) col2.keys[i];
				@SuppressWarnings("unchecked")
				V v = (V) col2.values[i];
				result = trieInsertWith(col2.hash, k, v, result, keyMerger, valueMerger);
			}
			return result;
		}

		PatriciaTrieBranch<K, V> b1 = (PatriciaTrieBranch<K, V>) t1;
		PatriciaTrieBranch<K, V> b2 = (PatriciaTrieBranch<K, V>) t2;

		if (b1.branchBit == b2.branchBit && b1.prefix == b2.prefix) {
			PatriciaTrieNode<K, V> l = trieUnion(b1.left, b2.left, keyMerger, valueMerger);
			PatriciaTrieNode<K, V> r = trieUnion(b1.right, b2.right, keyMerger, valueMerger);
			if (l == b1.left && r == b1.right)
				return t1;
			return new PatriciaTrieBranch<>(b1.prefix, b1.branchBit, l, r);
		}
		// b1 has the wider range and b2 fits entirely inside one of its
		// branches
		if (Integer.compareUnsigned(b1.branchBit, b2.branchBit) > 0
				&& matchPrefix(b2.prefix, b1.prefix, b1.branchBit)) {
			if (zeroBit(b2.prefix, b1.branchBit)) {
				PatriciaTrieNode<K, V> l = trieUnion(b1.left, t2, keyMerger, valueMerger);
				if (l == b1.left)
					return t1;
				return new PatriciaTrieBranch<>(b1.prefix, b1.branchBit, l, b1.right);
			} else {
				PatriciaTrieNode<K, V> r = trieUnion(b1.right, t2, keyMerger, valueMerger);
				if (r == b1.right)
					return t1;
				return new PatriciaTrieBranch<>(b1.prefix, b1.branchBit, b1.left, r);
			}
		}
		// b2 has the wider range and b1 fits entirely inside one of its
		// branches
		if (Integer.compareUnsigned(b2.branchBit, b1.branchBit) > 0
				&& matchPrefix(b1.prefix, b2.prefix, b2.branchBit)) {
			if (zeroBit(b1.prefix, b2.branchBit)) {
				PatriciaTrieNode<K, V> l = trieUnion(t1, b2.left, keyMerger, valueMerger);
				if (l == b2.left)
					return t2;
				return new PatriciaTrieBranch<>(b2.prefix, b2.branchBit, l, b2.right);
			} else {
				PatriciaTrieNode<K, V> r = trieUnion(t1, b2.right, keyMerger, valueMerger);
				if (r == b2.right)
					return t2;
				return new PatriciaTrieBranch<>(b2.prefix, b2.branchBit, b2.left, r);
			}
		}
		// Completely disjoint ranges: no key overlap, no merging needed
		return join(b1.prefix, t1, b2.prefix, t2);
	}

	/**
	 * Returns the intersection of {@code t1} and {@code t2}. Only keys present
	 * in both maps are kept; the result key is
	 * {@code keyMerger.apply(t1Key, t2Key)} and the result value is
	 * {@code valueMerger.apply(t1Value, t2Value)}.
	 * <p>
	 * If {@code t1 == t2} (reference equality), the node is returned as-is
	 * without recursion (structural-sharing short-circuit).
	 */
	private static <K, V> PatriciaTrieNode<K, V> trieIntersect(
			PatriciaTrieNode<K, V> t1,
			PatriciaTrieNode<K, V> t2,
			BiFunction<? super K, ? super K, ? extends K> keyMerger,
			BiFunction<? super V, ? super V, ? extends V> valueMerger) {
		if (t1 == null || t2 == null)
			return null;
		if (t1 == t2)
			return t1; // structural-sharing short-circuit

		if (t1 instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> l1 = (PatriciaTrieLeaf<K, V>) t1;
			Map.Entry<K, V> e2 = trieLookupEntry(l1.hash, l1.key, t2);
			if (e2 == null)
				return null;
			K mergedKey = keyMerger.apply(l1.key, e2.getKey());
			V mergedVal = valueMerger.apply(l1.value, e2.getValue());
			if (mergedKey == l1.key && Objects.equals(mergedVal, l1.value))
				return t1;
			return new PatriciaTrieLeaf<>(l1.hash, mergedKey, mergedVal);
		}
		if (t2 instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> l2 = (PatriciaTrieLeaf<K, V>) t2;
			Map.Entry<K, V> e1 = trieLookupEntry(l2.hash, l2.key, t1);
			if (e1 == null)
				return null;
			K mergedKey = keyMerger.apply(e1.getKey(), l2.key);
			V mergedVal = valueMerger.apply(e1.getValue(), l2.value);
			return new PatriciaTrieLeaf<>(l2.hash, mergedKey, mergedVal);
		}

		if (t1 instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col1 = (PatriciaTrieCollision<K, V>) t1;
			PatriciaTrieNode<K, V> result = null;
			for (int i = 0; i < col1.keys.length; i++) {
				@SuppressWarnings("unchecked")
				K k1 = (K) col1.keys[i];
				@SuppressWarnings("unchecked")
				V v1 = (V) col1.values[i];
				Map.Entry<K, V> e2 = trieLookupEntry(col1.hash, k1, t2);
				if (e2 != null) {
					K mergedKey = keyMerger.apply(k1, e2.getKey());
					V mergedVal = valueMerger.apply(v1, e2.getValue());
					result = (result == null)
							? new PatriciaTrieLeaf<>(col1.hash, mergedKey, mergedVal)
							: trieInsert(col1.hash, mergedKey, mergedVal, result);
				}
			}
			return result;
		}
		if (t2 instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col2 = (PatriciaTrieCollision<K, V>) t2;
			PatriciaTrieNode<K, V> result = null;
			for (int i = 0; i < col2.keys.length; i++) {
				@SuppressWarnings("unchecked")
				K k2 = (K) col2.keys[i];
				@SuppressWarnings("unchecked")
				V v2 = (V) col2.values[i];
				Map.Entry<K, V> e1 = trieLookupEntry(col2.hash, k2, t1);
				if (e1 != null) {
					K mergedKey = keyMerger.apply(e1.getKey(), k2);
					V mergedVal = valueMerger.apply(e1.getValue(), v2);
					result = (result == null)
							? new PatriciaTrieLeaf<>(col2.hash, mergedKey, mergedVal)
							: trieInsert(col2.hash, mergedKey, mergedVal, result);
				}
			}
			return result;
		}

		PatriciaTrieBranch<K, V> b1 = (PatriciaTrieBranch<K, V>) t1;
		PatriciaTrieBranch<K, V> b2 = (PatriciaTrieBranch<K, V>) t2;

		if (b1.branchBit == b2.branchBit && b1.prefix == b2.prefix) {
			return mkBranch(b1.prefix, b1.branchBit,
					trieIntersect(b1.left, b2.left, keyMerger, valueMerger),
					trieIntersect(b1.right, b2.right, keyMerger, valueMerger));
		}
		if (Integer.compareUnsigned(b1.branchBit, b2.branchBit) > 0
				&& matchPrefix(b2.prefix, b1.prefix, b1.branchBit)) {
			return zeroBit(b2.prefix, b1.branchBit)
					? trieIntersect(b1.left, t2, keyMerger, valueMerger)
					: trieIntersect(b1.right, t2, keyMerger, valueMerger);
		}
		if (Integer.compareUnsigned(b2.branchBit, b1.branchBit) > 0
				&& matchPrefix(b1.prefix, b2.prefix, b2.branchBit)) {
			return zeroBit(b1.prefix, b2.branchBit)
					? trieIntersect(t1, b2.left, keyMerger, valueMerger)
					: trieIntersect(t1, b2.right, keyMerger, valueMerger);
		}
		// Disjoint key ranges: empty intersection
		return null;
	}

	/**
	 * Returns true iff every key in {@code t1} is also present in {@code t2}
	 * and both {@code keyLeq.test(t1Key, t2Key)} and
	 * {@code valueLeq.test(t1Value, t2Value)} hold for each matched pair.
	 * <p>
	 * If {@code t1 == t2} (reference equality), returns {@code true}
	 * immediately without recursion (structural-sharing short-circuit).
	 */
	private static <K, V> boolean trieIsSubmapOf(
			PatriciaTrieNode<K, V> t1,
			PatriciaTrieNode<K, V> t2,
			BiPredicate<? super K, ? super K> keyLeq,
			BiPredicate<? super V, ? super V> valueLeq) {
		if (t1 == null)
			return true;
		if (t2 == null)
			return false;
		if (t1 == t2)
			return true; // structural-sharing short-circuit

		if (t1 instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> l1 = (PatriciaTrieLeaf<K, V>) t1;
			Map.Entry<K, V> e2 = trieLookupEntry(l1.hash, l1.key, t2);
			return e2 != null && keyLeq.test(l1.key, e2.getKey()) && valueLeq.test(l1.value, e2.getValue());
		}
		if (t1 instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col1 = (PatriciaTrieCollision<K, V>) t1;
			for (int i = 0; i < col1.keys.length; i++) {
				@SuppressWarnings("unchecked")
				K k = (K) col1.keys[i];
				@SuppressWarnings("unchecked")
				V v = (V) col1.values[i];
				Map.Entry<K, V> e2 = trieLookupEntry(col1.hash, k, t2);
				if (e2 == null || !keyLeq.test(k, e2.getKey()) || !valueLeq.test(v, e2.getValue()))
					return false;
			}
			return true;
		}

		// t1 is a PatriciaTreeBranch: it spans at least two distinct hash
		// regions.
		// A PatriciaTreeLeaf or PatriciaTreeCollision can cover at most one
		// hash value, so t1 cannot
		// be a submap of a PatriciaTreeLeaf or PatriciaTreeCollision.
		if (!(t2 instanceof PatriciaTrieBranch))
			return false;

		PatriciaTrieBranch<K, V> b1 = (PatriciaTrieBranch<K, V>) t1;
		PatriciaTrieBranch<K, V> b2 = (PatriciaTrieBranch<K, V>) t2;

		if (b1.branchBit == b2.branchBit && b1.prefix == b2.prefix)
			return trieIsSubmapOf(b1.left, b2.left, keyLeq, valueLeq)
					&& trieIsSubmapOf(b1.right, b2.right, keyLeq, valueLeq);

		// b2 covers a wider range and b1 fits entirely inside one of its
		// branches
		if (Integer.compareUnsigned(b2.branchBit, b1.branchBit) > 0
				&& matchPrefix(b1.prefix, b2.prefix, b2.branchBit)) {
			return zeroBit(b1.prefix, b2.branchBit)
					? trieIsSubmapOf(t1, b2.left, keyLeq, valueLeq)
					: trieIsSubmapOf(t1, b2.right, keyLeq, valueLeq);
		}
		// b1 is wider than b2, or they are disjoint: b1 has keys absent from b2
		return false;
	}

	// -----------------------------------------------------------------------
	// Entry-collection helper for iteration
	// -----------------------------------------------------------------------

	private static <K, V> void collectEntries(
			PatriciaTrieNode<K, V> node,
			List<Map.Entry<K, V>> out) {
		if (node == null)
			return;
		if (node instanceof PatriciaTrieLeaf) {
			PatriciaTrieLeaf<K, V> leaf = (PatriciaTrieLeaf<K, V>) node;
			out.add(new AbstractMap.SimpleImmutableEntry<>(leaf.key, leaf.value));
			return;
		}
		if (node instanceof PatriciaTrieCollision) {
			PatriciaTrieCollision<K, V> col = (PatriciaTrieCollision<K, V>) node;
			for (int i = 0; i < col.keys.length; i++) {
				@SuppressWarnings("unchecked")
				K k = (K) col.keys[i];
				@SuppressWarnings("unchecked")
				V v = (V) col.values[i];
				out.add(new AbstractMap.SimpleImmutableEntry<>(k, v));
			}
			return;
		}
		PatriciaTrieBranch<K, V> branch = (PatriciaTrieBranch<K, V>) node;
		collectEntries(branch.left, out);
		collectEntries(branch.right, out);
	}

	// -----------------------------------------------------------------------
	// PatriciaTrieMap public API
	// -----------------------------------------------------------------------

	private final PatriciaTrieNode<K, V> root;

	private PatriciaTrieMap(
			PatriciaTrieNode<K, V> root) {
		this.root = root;
	}

	@SuppressWarnings("rawtypes")
	private static final PatriciaTrieMap EMPTY = new PatriciaTrieMap<>(null);

	/**
	 * Returns the empty map.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 *
	 * @return the canonical empty {@code PatriciaTrieMap}
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> PatriciaTrieMap<K, V> empty() {
		return EMPTY;
	}

	/**
	 * Returns a map containing exactly one key-value pair.
	 *
	 * @param <K>   the key type
	 * @param <V>   the value type
	 * @param key   the key (may be {@code null})
	 * @param value the value (may be {@code null})
	 *
	 * @return a singleton {@code PatriciaTrieMap}
	 */
	public static <K, V> PatriciaTrieMap<K, V> singleton(
			K key,
			V value) {
		return new PatriciaTrieMap<>(new PatriciaTrieLeaf<>(Objects.hashCode(key), key, value));
	}

	/**
	 * Returns {@code true} if this map contains no key-value pairs.
	 *
	 * @return {@code true} iff this map is empty
	 */
	public boolean isEmpty() {
		return root == null;
	}

	/**
	 * Returns the number of key-value pairs in this map.
	 *
	 * @return the number of entries
	 */
	public int size() {
		return root == null ? 0 : root.size();
	}

	/**
	 * Returns {@code true} if this map contains a mapping for the given key.
	 * Key equality is determined via {@link Objects#equals}.
	 *
	 * @param key the key to search for (may be {@code null})
	 *
	 * @return {@code true} iff {@code key} is mapped
	 */
	public boolean containsKey(
			Object key) {
		return trieContainsKey(Objects.hashCode(key), key, root);
	}

	/**
	 * Returns the value to which the given key is mapped, or {@code null} if
	 * this map contains no mapping for the key.
	 * <p>
	 * A {@code null} return value can mean either that the key is absent or
	 * that it is explicitly mapped to {@code null}; use {@link #containsKey} to
	 * disambiguate.
	 *
	 * @param key the key to look up (may be {@code null})
	 *
	 * @return the mapped value, or {@code null} if absent
	 */
	public V get(
			Object key) {
		return trieLookup(Objects.hashCode(key), key, root);
	}

	/**
	 * Returns the value to which the given key is mapped, or
	 * {@code defaultValue} if this map contains no mapping for the key.
	 *
	 * @param key          the key to look up (may be {@code null})
	 * @param defaultValue the value to return if {@code key} is absent (may be
	 *                         {@code null})
	 *
	 * @return the mapped value, or {@code defaultValue} if absent
	 */
	public V getOrDefault(
			Object key,
			V defaultValue) {
		V trieLookup = trieLookup(Objects.hashCode(key), key, root);
		if (trieLookup == null)
			return defaultValue;
		return trieLookup;
	}

	/**
	 * Returns a new map that is identical to this one except that {@code key}
	 * is mapped to {@code value}. If {@code key} was already present its old
	 * value is discarded.
	 * <p>
	 * This map is not modified.
	 *
	 * @param key   the key to insert or update (may be {@code null})
	 * @param value the value to associate with {@code key} (may be
	 *                  {@code null})
	 *
	 * @return the updated map (may be {@code this} if nothing changed)
	 */
	public PatriciaTrieMap<K, V> put(
			K key,
			V value) {
		PatriciaTrieNode<K, V> newRoot = trieInsert(Objects.hashCode(key), key, value, root);
		return newRoot == root ? this : new PatriciaTrieMap<>(newRoot);
	}

	/**
	 * Returns a new map that is identical to this one except that any mapping
	 * for {@code key} has been removed. If {@code key} was not present, returns
	 * {@code this} unchanged.
	 * <p>
	 * This map is not modified.
	 *
	 * @param key the key to remove (may be {@code null})
	 *
	 * @return the updated map (may be {@code this} if the key was absent)
	 */
	public PatriciaTrieMap<K, V> remove(
			Object key) {
		PatriciaTrieNode<K, V> newRoot = trieRemove(Objects.hashCode(key), key, root);
		return newRoot == root ? this : new PatriciaTrieMap<>(newRoot);
	}

	/**
	 * Returns the union of this map and {@code other}. The result contains all
	 * keys from both maps. For keys present in both, the stored key object is
	 * chosen by {@code keyMerger.apply(thisKey, otherKey)} and the value is
	 * produced by {@code valueMerger.apply(thisValue, otherValue)}.
	 * <p>
	 * When {@code this == other} (reference equality), returns {@code this}
	 * immediately. Sub-tries that are shared between the two maps are also
	 * reused without recursion, making this operation efficient for maps that
	 * were derived from one another (as is common in fixed-point iteration).
	 * <p>
	 * Neither map is modified.
	 *
	 * @param other       the map to merge with
	 * @param keyMerger   called with {@code (thisKey, otherKey)} for keys
	 *                        present in both maps to select the result key
	 *                        object; must not be {@code null}
	 * @param valueMerger called with {@code (thisValue, otherValue)} for keys
	 *                        present in both maps; must not be {@code null}
	 *
	 * @return the union map
	 */
	public PatriciaTrieMap<K, V> union(
			PatriciaTrieMap<K, V> other,
			BiFunction<? super K, ? super K, ? extends K> keyMerger,
			BiFunction<? super V, ? super V, ? extends V> valueMerger) {
		PatriciaTrieNode<K, V> newRoot = trieUnion(this.root, other.root, keyMerger, valueMerger);
		if (newRoot == this.root)
			return this;
		if (newRoot == other.root)
			return other;
		return new PatriciaTrieMap<>(newRoot);
	}

	/**
	 * Returns the union of this map and {@code other}. The result contains all
	 * keys from both maps. For keys present in both, {@code valueMerger} is
	 * called with {@code (thisValue, otherValue)} to produce the combined
	 * value; the key object from {@code this} is kept.
	 * <p>
	 * When {@code this == other} (reference equality), returns {@code this}
	 * immediately. Sub-tries shared between the two maps are reused without
	 * recursion.
	 * <p>
	 * Neither map is modified.
	 *
	 * @param other       the map to merge with
	 * @param valueMerger called with {@code (thisValue, otherValue)} for keys
	 *                        present in both maps; must not be {@code null}
	 *
	 * @return the union map
	 */
	public PatriciaTrieMap<K, V> union(
			PatriciaTrieMap<K, V> other,
			BiFunction<? super V, ? super V, ? extends V> valueMerger) {
		return union(other, (
				k1,
				k2) -> k1, valueMerger);
	}

	/**
	 * Returns the intersection of this map and {@code other}. The result
	 * contains only keys present in <em>both</em> maps. For each shared key,
	 * the stored key object is chosen by {@code keyMerger.apply(thisKey,
	 * otherKey)} and the value by {@code valueMerger.apply(thisValue,
	 * otherValue)}.
	 * <p>
	 * When {@code this == other} (reference equality), returns {@code this}
	 * immediately. Sub-tries shared between the two maps are reused without
	 * recursion.
	 * <p>
	 * Neither map is modified.
	 *
	 * @param other       the map to intersect with
	 * @param keyMerger   called with {@code (thisKey, otherKey)} for each
	 *                        shared key to select the result key object; must
	 *                        not be {@code null}
	 * @param valueMerger called with {@code (thisValue, otherValue)} for each
	 *                        shared key; must not be {@code null}
	 *
	 * @return the intersection map
	 */
	public PatriciaTrieMap<K, V> intersection(
			PatriciaTrieMap<K, V> other,
			BiFunction<? super K, ? super K, ? extends K> keyMerger,
			BiFunction<? super V, ? super V, ? extends V> valueMerger) {
		PatriciaTrieNode<K, V> newRoot = trieIntersect(this.root, other.root, keyMerger, valueMerger);
		return newRoot == this.root ? this : new PatriciaTrieMap<>(newRoot);
	}

	/**
	 * Returns the intersection of this map and {@code other}. The result
	 * contains only keys present in <em>both</em> maps, with values combined by
	 * {@code valueMerger.apply(thisValue, otherValue)}; the key object from
	 * {@code this} is kept.
	 * <p>
	 * When {@code this == other} (reference equality), returns {@code this}
	 * immediately. Sub-tries shared between the two maps are reused without
	 * recursion.
	 * <p>
	 * Neither map is modified.
	 *
	 * @param other       the map to intersect with
	 * @param valueMerger called with {@code (thisValue, otherValue)} for each
	 *                        shared key; must not be {@code null}
	 *
	 * @return the intersection map
	 */
	public PatriciaTrieMap<K, V> intersection(
			PatriciaTrieMap<K, V> other,
			BiFunction<? super V, ? super V, ? extends V> valueMerger) {
		return intersection(other, (
				k1,
				k2) -> k1, valueMerger);
	}

	/**
	 * Returns {@code true} iff every key in this map is also present in
	 * {@code other} and both {@code keyLeq.test(thisKey, otherKey)} and
	 * {@code valueLeq.test(thisValue, otherValue)} hold for every matched pair.
	 * <p>
	 * This is the pointwise partial order on maps extended to cover key objects
	 * whose {@link Object#equals} treats them as equal but whose internal state
	 * may still differ (e.g. a strength flag). The key predicate lets callers
	 * reject a match even when both keys compare equal.
	 * <p>
	 * When {@code this == other} (reference equality), returns {@code true}
	 * immediately. Sub-tries shared between the two maps are also
	 * short-circuited.
	 *
	 * @param other    the map to compare against
	 * @param keyLeq   called with {@code (thisKey, otherKey)} for each key
	 *                     present in both maps; must not be {@code null}
	 * @param valueLeq called with {@code (thisValue, otherValue)}; must not be
	 *                     {@code null}
	 *
	 * @return {@code true} iff this map is pointwise ≤ {@code other}
	 */
	public boolean isSubmapOf(
			PatriciaTrieMap<K, V> other,
			BiPredicate<? super K, ? super K> keyLeq,
			BiPredicate<? super V, ? super V> valueLeq) {
		return trieIsSubmapOf(this.root, other.root, keyLeq, valueLeq);
	}

	/**
	 * Returns {@code true} iff every key in this map is also present in
	 * {@code other} and {@code valueLeq.test(thisValue, otherValue)} holds for
	 * every such key.
	 * <p>
	 * This corresponds to the pointwise partial order on maps (treating absent
	 * keys as an implicit minimum element).
	 * <p>
	 * When {@code this == other} (reference equality), returns {@code true}
	 * immediately. Sub-tries shared between the two maps are also
	 * short-circuited.
	 *
	 * @param other    the map to compare against
	 * @param valueLeq called with {@code (thisValue, otherValue)}; must not be
	 *                     {@code null}
	 *
	 * @return {@code true} iff this map is pointwise ≤ {@code other}
	 */
	public boolean isSubmapOf(
			PatriciaTrieMap<K, V> other,
			BiPredicate<? super V, ? super V> valueLeq) {
		return isSubmapOf(other, (
				k1,
				k2) -> true, valueLeq);
	}

	/**
	 * Returns an unmodifiable {@link Set} view of the keys in this map.
	 *
	 * @return the key set
	 */
	public Set<K> keySet() {
		Set<K> keys = new HashSet<>(size() * 2);
		for (Map.Entry<K, V> e : this)
			keys.add(e.getKey());
		return Collections.unmodifiableSet(keys);
	}

	/**
	 * Returns an unmodifiable {@link Collection} view of the values in this
	 * map.
	 *
	 * @return the values
	 */
	public Collection<V> values() {
		List<V> vals = new ArrayList<>(size());
		for (Map.Entry<K, V> e : this)
			vals.add(e.getValue());
		return Collections.unmodifiableList(vals);
	}

	/**
	 * Returns an unmodifiable {@link Set} view of the key-value pairs in this
	 * map.
	 *
	 * @return the entry set
	 */
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> entries = new HashSet<>(size() * 2);
		for (Map.Entry<K, V> e : this)
			entries.add(e);
		return Collections.unmodifiableSet(entries);
	}

	/**
	 * Performs the given action for each key-value pair in this map.
	 *
	 * @param action the action to perform; must not be {@code null}
	 */
	public void forEach(
			BiConsumer<? super K, ? super V> action) {
		for (Map.Entry<K, V> e : this)
			action.accept(e.getKey(), e.getValue());
	}

	/**
	 * Returns an {@link Iterator} over the entries of this map. The iteration
	 * order is determined by the bit structure of the keys' hash codes and is
	 * not otherwise specified.
	 * <p>
	 * The returned iterator does not support {@link Iterator#remove()}.
	 *
	 * @return an iterator over the map entries
	 */
	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		List<Map.Entry<K, V>> entries = new ArrayList<>(size());
		collectEntries(root, entries);
		return Collections.unmodifiableList(entries).iterator();
	}

	/**
	 * Returns {@code true} iff {@code obj} is a {@code PatriciaTrieMap} with
	 * exactly the same key-value pairs as this map.
	 *
	 * @param obj the object to compare
	 *
	 * @return {@code true} iff the two maps are equal
	 */
	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof PatriciaTrieMap))
			return false;
		PatriciaTrieMap<?, ?> other = (PatriciaTrieMap<?, ?>) obj;
		if (size() != other.size())
			return false;
		if (root == other.root)
			return true; // structural-sharing short-circuit
		if (root == null || other.root == null)
			return false;
		return root.equals(other.root);
	}

	/**
	 * Returns a hash code consistent with {@link #equals}. The hash code is the
	 * sum of {@code key.hashCode() ^ value.hashCode()} over all entries,
	 * matching the contract of {@link java.util.Map#hashCode()}.
	 *
	 * @return the hash code for this map
	 */
	@Override
	public int hashCode() {
		int h = 0;
		for (Map.Entry<K, V> e : this)
			h += Objects.hashCode(e.getKey()) ^ Objects.hashCode(e.getValue());
		return h;
	}

	/**
	 * Returns a human-readable string representation of this map in the form
	 * {@code {k1=v1, k2=v2, ...}}.
	 *
	 * @return a string representation of this map
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<K, V> e : this) {
			if (!first)
				sb.append(", ");
			sb.append(e.getKey()).append('=').append(e.getValue());
			first = false;
		}
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Returns a new {@link HashMap} containing the same key-value pairs as this
	 * map.
	 *
	 * @return a new {@code HashMap} with the same entries
	 */
	public Map<K, V> toHashMap() {
		HashMap<K, V> map = new HashMap<>();
		for (Map.Entry<K, V> e : this)
			map.put(e.getKey(), e.getValue());
		return map;
	}
}
