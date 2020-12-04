package it.unive.lisa.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * A set of elements that are stored externally from this set. Elements are
 * stored inside an {@link ExternalSetCache} instance that is shared among all
 * the sets created from that instance. This avoid the duplication of the
 * references to the elements, enabling this class' instances to store the
 * indexes of the elements inside the cache instead of the full memory address.
 * The indexes are stored through bit vectors, enabling better memory
 * efficiency.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of elements inside this set
 */
public class ExternalSet<T> implements Iterable<T> {

	private static final int LENGTH_MASK = 0x3f;

	/**
	 * Yields a mask of {@code 0}s with one 1, represented as a long value, to
	 * represent the given number inside a long bitvector. To determine the
	 * correct long bitvector to apply this mask to, use
	 * {@link #bitvector_index(int)}
	 * 
	 * @param n the number
	 * 
	 * @return the bitwise mask
	 */
	private static long bitmask(int n) {
		// assuming that n will be stored in the right long (obtained with
		// toNLongs(n)),
		// we have to determine which bit of the long has to be turned to 1. To
		// do that,
		// we take the 1L (that is just the right-most bit set to 1) and we
		// shift it to
		// the left. The amount of positions that we need to shift it is equal
		// to n%64
		// (we use bitwise and as a mask) that yields the correct bit to
		// represent a
		// number between 0 and 63 inside the long
		return 1L << (n & LENGTH_MASK);
	}

	/**
	 * Yields the 0-based index of the bitvector where the bit representing the
	 * given number lies.
	 * 
	 * @param n the number
	 * 
	 * @return the index
	 */
	private static int bitvector_index(int n) {
		// a long is 64 (2^6) bits, and can thus represent 64 elements.
		// shifting n to the right by 6 determines the number of
		// 64-bits chunks needed to represent that number.
		return n >> 6;
	}

	private static void set(long[] bits, int n) {
		bits[bitvector_index(n)] |= bitmask(n);
	}

	private static void unset(long[] bits, int n) {
		bits[bitvector_index(n)] &= ~bitmask(n);
	}

	private static boolean isset(long bitvector, int pos) {
		return (bitvector & bitmask(pos)) != 0L;
	}

	/**
	 * The bits representing the set. If a bit is 1 then the corresponding
	 * element of the cache is in the set.
	 */
	private long[] bits;

	/**
	 * The cache that generated this set and that contains the elements of this
	 * set.
	 */
	private final ExternalSetCache<T> cache;

	/**
	 * Builds an empty set connected to the given cache.
	 * 
	 * @param cache the cache
	 */
	protected ExternalSet(ExternalSetCache<T> cache) {
		this.bits = new long[1];
		this.cache = cache;
	}

	/**
	 * Builds a set with the given bits and cache.
	 * 
	 * @param bits  the bits
	 * @param cache the cache
	 */
	protected ExternalSet(long[] bits, ExternalSetCache<T> cache) {
		this.bits = bits;
		this.cache = cache;
	}

	/**
	 * Builds a clone of another set, connected to the same cache.
	 * 
	 * @param other the other set
	 */
	protected ExternalSet(ExternalSet<T> other) {
		this.bits = other.bits.clone();
		this.cache = other.cache;
	}

	/**
	 * Builds a set containing exactly one element and connected to the given
	 * cache.
	 * 
	 * @param cache   the cache
	 * @param element the element
	 */
	protected ExternalSet(ExternalSetCache<T> cache, T element) {
		this.cache = cache;
		int pos = cache.indexOfOrAdd(element);
		bits = new long[1 + bitvector_index(pos)];
		set(bits, pos);
	}

	/**
	 * Builds a set containing all the elements of the given iterable and
	 * connected to the given cache.
	 * 
	 * @param cache    the cache that must be used for this set
	 * @param elements the elements put inside the set
	 */
	protected ExternalSet(ExternalSetCache<T> cache, Iterable<T> elements) {
		this(cache);
		for (T e : elements)
			add(e);
	}

	/**
	 * Yields the cache that this set is connected to.
	 * 
	 * @return the cache
	 */
	public ExternalSetCache<T> getCache() {
		return cache;
	}

	/**
	 * Adds the given element to this set.
	 * 
	 * @param e the element to add
	 */
	public void add(T e) {
		long[] localbits = bits;
		int pos = cache.indexOfOrAdd(e);
		int bitvector = bitvector_index(pos);

		if (bitvector >= localbits.length) {
			// the array is not long enough for the position
			// of the element that we are adding
			bits = new long[1 + bitvector];
			System.arraycopy(localbits, 0, bits, 0, localbits.length);
			set(bits, pos);
		} else
			set(localbits, pos);
	}

	/**
	 * Removes the given element from this set.
	 * 
	 * @param e the element to remove
	 */
	public void remove(T e) {
		int pos = getCache().indexOf(e);
		if (pos < 0)
			return;

		long[] localbits = this.bits;
		if (bitvector_index(pos) >= localbits.length)
			return;

		unset(localbits, pos);
		removeTrailingZeros();
	}

	/**
	 * Yields the number of elements in this set.
	 * 
	 * @return the number of elements in this set
	 */
	public int size() {
		long bitvector;
		int count = 0;
		long[] localbits = this.bits;

		for (int pos = localbits.length - 1; pos >= 0; pos--) {
			bitvector = localbits[pos];
			if (bitvector != 0L)
				for (int i = 0; i < 64; i++)
					// we iterate over all possible bits of the bitvector
					if (isset(bitvector, i))
						count++;
		}

		return count;
	}

	/**
	 * Determines if this set is empty.
	 * 
	 * @return true if and only if this set is empty
	 */
	public final boolean isEmpty() {
		removeTrailingZeros();
		return bits.length == 1 && bits[0] == 0L;
	}

	private void removeTrailingZeros() {
		long[] localbits = bits;
		int length = localbits.length;

		// we search for the right-most bitvector that has at least one
		// bit set to 1, excluding the first one since if even if it is
		// zero we have to leave it as it is (at least one bitvector is needed)
		while (length > 1 && localbits[length - 1] == 0L)
			length--;

		if (length != localbits.length) {
			// if we decreased at least once, we can shrink the bits
			bits = new long[length];
			System.arraycopy(localbits, 0, bits, 0, length);
		}
	}

	@Override
	public final Iterator<T> iterator() {
		return new BitSetIterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bits);
		return result;
	}

	/**
	 * Determines if this set is equal to the given one, that is, if they share
	 * the cache (checked through reference equality) and contains the same
	 * elements. <br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (obj.getClass() != getClass())
			return false;
		ExternalSet<?> other = (ExternalSet<?>) obj;
		if (cache != other.cache)
			return false;
		if (!Arrays.equals(bits, other.bits))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return "[" + StringUtils.join(this, ", ") + "]";
	}

	/**
	 * Removes all elements from this set.
	 */
	public void clear() {
		this.bits = new long[1];
	}

	/**
	 * Yields a concrete list containing all the elements corresponding to the
	 * bits in this set.
	 * 
	 * @return the collected elements
	 */
	public List<T> collect() {
		List<T> list = new ArrayList<>();
		for (T e : this)
			list.add(e);
		return list;
	}

	/**
	 * Yields a fresh copy of this set, defined over the same cache and
	 * containing the same elements.
	 * 
	 * @return the fresh copy
	 */
	public ExternalSet<T> copy() {
		return new ExternalSet<>(this);
	}

	/**
	 * Determines if an element belongs to this set.
	 * 
	 * @param e the element
	 * 
	 * @return true if and only if {@code e} is in this set
	 */
	public final boolean contains(T e) {
		int pos = cache.indexOf(e);
		if (pos < 0)
			// if it's not inside the cache, it's not in the set
			return false;

		int bitvector = bitvector_index(pos);
		return bitvector < bits.length && isset(bits[bitvector], pos);
	}

	/**
	 * Determines if this set contains all elements of another if they share the
	 * same cache.
	 * 
	 * @param other the other set
	 * 
	 * @return {@code true} if and only if {@code other} is included into this
	 *             set
	 */
	public boolean contains(ExternalSet<T> other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (cache != other.cache)
			return false;

		long[] otherbits = other.bits, localbits = bits;
		if (otherbits.length > localbits.length)
			// clever check: this cannot contain other if other is bigger
			return false;

		// if at least one bit that is 0 in this is 1 in other, than this does
		// not
		// contain other
		for (int i = otherbits.length; i >= 0; i--)
			if ((localbits[i] | otherbits[i]) != localbits[i])
				return false;

		return true;
	}

	/**
	 * Determines if this set has at least an element in common with another if
	 * they share the same cache.
	 * 
	 * @param other the other set
	 * 
	 * @return true if and only if this set intersects the other
	 */
	public final boolean intersects(ExternalSet<T> other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (cache != other.cache)
			return false;

		// if at least one bit is one on both, then the two intersect
		long[] otherbits = other.bits, localbits = bits;
		int min = (otherbits.length > localbits.length) ? localbits.length : otherbits.length;
		for (int i = min - 1; i >= 0; i--)
			if ((localbits[i] & otherbits[i]) != 0L)
				return true;

		return false;
	}

	/**
	 * Yields the intersection of this set and another. Neither of them gets
	 * modified. If {@code other} is {@code null}, or if the two sets are not
	 * defined over the same cache, this set is returned.
	 * 
	 * @param other the other set
	 * 
	 * @return the intersection of the two sets
	 */
	public ExternalSet<T> intersection(ExternalSet<T> other) {
		if (this == other)
			return this;
		if (other == null)
			return this;
		if (cache != other.cache)
			return this;

		int index;
		long[] otherbits;
		ExternalSet<T> result;

		// copy the shortest one, then perform bitwise and with the longest one
		// on
		// common bits
		if (this.bits.length > other.bits.length) {
			otherbits = this.bits;
			index = other.bits.length;
			result = new ExternalSet<>(other);
		} else {
			otherbits = other.bits;
			index = bits.length;
			result = new ExternalSet<>(this);
		}

		long[] res = result.bits;
		while (index > 0)
			res[--index] &= otherbits[index];
		result.removeTrailingZeros();
		return result;
	}

	/**
	 * Yields a new set obtained from this by removing the given elements. If
	 * {@code other} is {@code null}, or if the two sets are not defined over
	 * the same cache, this set is returned.
	 * 
	 * @param other the elements to remove
	 * 
	 * @return a set obtained from this by removing the elements in
	 *             {@code other}
	 */
	public ExternalSet<T> difference(ExternalSet<T> other) {
		if (this == other)
			return this;
		if (other == null)
			return this;
		if (cache != other.cache)
			return this;

		long[] localbits = this.bits;
		int pos = localbits.length;
		ExternalSet<T> result = new ExternalSet<>(this);
		long[] otherbits = other.bits, res = result.bits;

		if (otherbits.length < pos)
			pos = otherbits.length;

		while (--pos >= 0)
			res[pos] &= ~otherbits[pos];

		result.removeTrailingZeros();
		return result;
	}

	/**
	 * Yields the union of this set and another. Neither of them gets modified.
	 * If {@code other} is {@code null}, or if the two sets are not defined over
	 * the same cache, this set is returned.
	 * 
	 * @param other the other set
	 * 
	 * @return the union of this set and {@code other}
	 */
	public ExternalSet<T> union(ExternalSet<T> other) {
		if (this == other)
			return this;
		if (other == null)
			return this;
		if (cache != other.cache)
			return this;

		long[] localbits = this.bits, otherbits = other.bits;
		int thislength = localbits.length, otherlength = otherbits.length;
		ExternalSet<T> result;

		if (thislength < otherlength) {
			result = new ExternalSet<>(other);
			long[] res = result.bits;
			for (--thislength; thislength >= 0; thislength--)
				res[thislength] |= localbits[thislength];
		} else {
			result = new ExternalSet<>(this);
			long[] res = result.bits;
			while (--otherlength >= 0)
				res[otherlength] |= otherbits[otherlength];
		}

		return result;
	}

	/**
	 * An iterator over the elements of a {@link ExternalSet}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	private class BitSetIterator implements Iterator<T> {

		/**
		 * The next bit to look at
		 */
		private int next;

		/**
		 * The bits to iterate over
		 */
		private final long[] bits;

		/**
		 * The total number of bits to iterate over
		 */
		private final int totalBits;

		/**
		 * Builds an iterator from the given bits and elements.
		 */
		private BitSetIterator() {
			this.bits = ExternalSet.this.bits;
			this.totalBits = bits.length << 6; // we go back to the integer
												// representation
			this.next = findNextBit();
		}

		/**
		 * Yields the next non-ZERO bit in the set.
		 * 
		 * @return the position of the next non-zero bit in the set, starting at
		 *             {@code start} non-inclusive, or {@code -1}
		 */
		private int findNextBit() {
			long[] localbits = this.bits;
			int start = next;
			int l = this.totalBits;

			while (start < l) {
				int pos = bitvector_index(start);
				long bitvector = localbits[pos];

				while (bitvector == 0L) {
					// we can skip to the next vector
					if ((start += 64) >= l)
						return -1;
					bitvector = localbits[++pos];
				}

				long bit = bitmask(start);
				do {
					if ((bitvector & bit) != 0L)
						return start;

					bit <<= 1;
					start++;
				} while (bit != 0);
			}

			return -1;
		}

		@Override
		public boolean hasNext() {
			return next >= 0;
		}

		@Override
		public T next() {
			int nb = next++;
			next = findNextBit();

			return cache.get(nb);
		}

		/**
		 * Removal is not supported!
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException("Removal from a bitset is not supported");
		}
	}
}