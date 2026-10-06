package it.unive.lisa.util.datastructures.trie;

import java.util.Arrays;
import java.util.Objects;

/**
 * A {@link PatriciaTrieNode} collision node that holds two or more entries that
 * all share the same hash code. Key equality inside this node is determined via
 * {@link Object#equals}. Keys and values are stored in parallel arrays; the
 * arrays always have length ≥ 2. See {@link PatriciaTrieMap}s' javadoc for more
 * information.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public final class PatriciaTrieCollision<K, V>
		extends
		PatriciaTrieNode<K, V> {

	/**
	 * The hash of all keys stored in this collision node.
	 */
	public final int hash;

	/**
	 * The keys stored in this collision node.
	 */
	public final Object[] keys;

	/**
	 * The values stored in this collision node.
	 */
	public final Object[] values;

	/**
	 * Builds a collision node.
	 * 
	 * @param hash   the hash of all keys stored in this collision node
	 * @param keys   the keys stored in this collision node
	 * @param values the values stored in this collision node
	 */
	public PatriciaTrieCollision(
			int hash,
			Object[] keys,
			Object[] values) {
		this.hash = hash;
		this.keys = keys;
		this.values = values;
	}

	@Override
	public int size() {
		return keys.length;
	}

	/**
	 * Returns the value associated with {@code key}, or {@code null} if no such
	 * mapping exists.
	 * 
	 * @param key the key whose associated value is to be returned
	 * 
	 * @return the value associated with {@code key}, or {@code null} if no such
	 *             mapping exists
	 */
	@SuppressWarnings("unchecked")
	public V get(
			Object key) {
		for (int i = 0; i < keys.length; i++)
			if (Objects.equals(keys[i], key))
				return (V) values[i];
		return null;
	}

	/**
	 * Returns {@code true} if this node contains a mapping for {@code key}.
	 * 
	 * @param key the key whose presence in this node is to be tested
	 * 
	 * @return {@code true} if this node contains a mapping for {@code key}
	 */
	public boolean containsKey(
			Object key) {
		for (Object k : keys)
			if (Objects.equals(k, key))
				return true;
		return false;
	}

	/**
	 * Returns a new {@link PatriciaTrieCollision} with {@code key} mapped to
	 * {@code value}. If {@code key} already exists, its value is replaced.
	 *
	 * @param key   the key to add
	 * @param value the value to associate with {@code key}
	 * 
	 * @return a new {@link PatriciaTrieCollision} with {@code key} mapped to
	 *             {@code value}
	 */
	public PatriciaTrieCollision<K, V> with(
			K key,
			V value) {
		for (int i = 0; i < keys.length; i++) {
			if (Objects.equals(keys[i], key)) {
				if (Objects.equals(values[i], value))
					return this;
				Object[] nv = values.clone();
				nv[i] = value;
				return new PatriciaTrieCollision<>(hash, keys, nv);
			}
		}
		Object[] nk = new Object[keys.length + 1];
		Object[] nv = new Object[values.length + 1];
		System.arraycopy(keys, 0, nk, 0, keys.length);
		System.arraycopy(values, 0, nv, 0, values.length);
		nk[keys.length] = key;
		nv[values.length] = value;
		return new PatriciaTrieCollision<>(hash, nk, nv);
	}

	/**
	 * Returns a node with {@code key} removed. Returns {@code null} if the
	 * result would be empty. Degenerates to a {@link PatriciaTrieLeaf} if only
	 * one entry remains.
	 *
	 * @param key the key to remove
	 * 
	 * @return a node with {@code key} removed, or {@code null} if the result
	 *             would be empty
	 */
	@SuppressWarnings("unchecked")
	public PatriciaTrieNode<K, V> without(
			K key) {
		int idx = -1;
		for (int i = 0; i < keys.length; i++)
			if (Objects.equals(keys[i], key)) {
				idx = i;
				break;
			}
		if (idx < 0)
			return this;
		if (keys.length == 1)
			return null;
		if (keys.length == 2) {
			int survivor = idx == 0 ? 1 : 0;
			return new PatriciaTrieLeaf<>(hash, (K) keys[survivor], (V) values[survivor]);
		}
		Object[] nk = new Object[keys.length - 1];
		Object[] nv = new Object[values.length - 1];
		System.arraycopy(keys, 0, nk, 0, idx);
		System.arraycopy(keys, idx + 1, nk, idx, keys.length - idx - 1);
		System.arraycopy(values, 0, nv, 0, idx);
		System.arraycopy(values, idx + 1, nv, idx, values.length - idx - 1);
		return new PatriciaTrieCollision<>(hash, nk, nv);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + hash;
		result = prime * result + Arrays.deepHashCode(keys);
		result = prime * result + Arrays.deepHashCode(values);
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PatriciaTrieCollision<?, ?> other = (PatriciaTrieCollision<?, ?>) obj;
		if (hash != other.hash)
			return false;
		if (!Arrays.deepEquals(keys, other.keys))
			return false;
		if (!Arrays.deepEquals(values, other.values))
			return false;
		return true;
	}
}
