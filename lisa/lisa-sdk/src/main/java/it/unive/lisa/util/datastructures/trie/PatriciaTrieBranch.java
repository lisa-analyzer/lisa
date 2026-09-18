package it.unive.lisa.util.datastructures.trie;

/**
 * An internal {@link PatriciaTrieNode} branch node. All keys in {@code left}
 * have a 0-bit at position {@code branchBit}; all keys in {@code right} have a
 * 1-bit there. The field {@code prefix} stores the common bits of all keys in
 * this subtree above {@code branchBit} (bits at and below {@code branchBit} are
 * zero in {@code prefix}). See {@link PatriciaTrieMap}s' javadoc for more
 * information.
 * <p>
 * {@code branchBit} is always a power of two (exactly one bit set). Both
 * {@code left} and {@code right} are guaranteed non-null; branch nodes with a
 * single non-null child are never created (path compression).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public final class PatriciaTrieBranch<K, V>
		extends
		PatriciaTrieNode<K, V> {

	/**
	 * The common bits of all keys in this subtree above {@code branchBit} (bits
	 * at and below {@code branchBit} are zero in {@code prefix}).
	 */
	public final int prefix;

	/**
	 * The bit position that is used to branch {@code left} and {@code right}.
	 * All keys in {@code left} have a 0-bit at position {@code branchBit}; all
	 * keys in {@code right} have a 1-bit there.
	 */
	public final int branchBit;

	/**
	 * The left child of this branch node. All keys in {@code left} have a 0-bit
	 * at position {@code branchBit}.
	 */
	public final PatriciaTrieNode<K, V> left;

	/**
	 * The right child of this branch node. All keys in {@code right} have a
	 * 1-bit at position {@code branchBit}.
	 */
	public final PatriciaTrieNode<K, V> right;

	private final int size;

	/**
	 * Builds a branch node.
	 * 
	 * @param prefix    the common bits of all keys in this subtree above
	 *                      {@code branchBit} (bits at and below
	 *                      {@code branchBit} are zero in {@code prefix})
	 * @param branchBit the bit position that is used to branch {@code left} and
	 *                      {@code right}. All keys in {@code left} have a 0-bit
	 *                      at position {@code branchBit}; all keys in
	 *                      {@code right} have a 1-bit there
	 * @param left      the left child of this branch node. All keys in
	 *                      {@code left} have a 0-bit at position
	 *                      {@code branchBit}
	 * @param right     the right child of this branch node. All keys in
	 *                      {@code right} have a 1-bit at position
	 *                      {@code branchBit}
	 */
	public PatriciaTrieBranch(
			int prefix,
			int branchBit,
			PatriciaTrieNode<K, V> left,
			PatriciaTrieNode<K, V> right) {
		this.prefix = prefix;
		this.branchBit = branchBit;
		this.left = left;
		this.right = right;
		this.size = left.size() + right.size();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + prefix;
		result = prime * result + branchBit;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
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
		PatriciaTrieBranch<?, ?> other = (PatriciaTrieBranch<?, ?>) obj;
		if (prefix != other.prefix)
			return false;
		if (branchBit != other.branchBit)
			return false;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}
}
