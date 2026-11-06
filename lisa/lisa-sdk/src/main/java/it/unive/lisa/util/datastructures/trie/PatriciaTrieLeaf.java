package it.unive.lisa.util.datastructures.trie;

import java.util.Objects;

/**
 * A leaf {@link PatriciaTrieNode} that holds exactly one key-value pair. The
 * stored {@code hash} field is the value of {@link Objects#hashCode(Object)}
 * for the key. Consult {@link PatriciaTrieMap}s' javadoc for more information.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public final class PatriciaTrieLeaf<K, V>
		extends
		PatriciaTrieNode<K, V> {

	/**
	 * The hash of the key stored in this leaf.
	 */
	public final int hash;

	/**
	 * The key stored in this leaf.
	 */
	public final K key;

	/**
	 * The value stored in this leaf.
	 */
	public final V value;

	/**
	 * Builds a leaf node.
	 * 
	 * @param hash  the hash of the key stored in this leaf
	 * @param key   the key stored in this leaf
	 * @param value the value stored in this leaf
	 */
	public PatriciaTrieLeaf(
			int hash,
			K key,
			V value) {
		this.hash = hash;
		this.key = key;
		this.value = value;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + hash;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		PatriciaTrieLeaf<?, ?> other = (PatriciaTrieLeaf<?, ?>) obj;
		if (hash != other.hash)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

}
