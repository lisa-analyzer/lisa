package it.unive.lisa.util.datastructures.trie;

/**
 * Abstract base for all trie nodes. Consult {@link PatriciaTrieMap}s' javadoc
 * for more information.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public abstract class PatriciaTrieNode<K, V> {

	/**
	 * Returns the number of key-value pairs stored under this node.
	 * 
	 * @return the size of the node
	 */
	public abstract int size();

	@Override
	public abstract boolean equals(
			Object obj);

	@Override
	public abstract int hashCode();
}
