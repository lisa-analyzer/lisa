package it.unive.lisa.util.collections.externalSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * An {@link ExternalSet} where the indexes of the elements included in the set
 * are stored through bit vectors, enabling better memory efficiency.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of elements inside this set
 */
public final class BitExternalSet<T>
		implements
		ExternalSet<T> {

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
	BitExternalSet(
			ExternalSetCache<T> cache) {
		this.bits = new long[1];
		this.cache = cache;
	}

	/**
	 * Builds a set with the given bits and cache.
	 * 
	 * @param bits  the bits
	 * @param cache the cache
	 */
	BitExternalSet(
			long[] bits,
			ExternalSetCache<T> cache) {
		this.bits = bits;
		this.cache = cache;
	}

	/**
	 * Builds a clone of another set, connected to the same cache.
	 * 
	 * @param other the other set
	 */
	BitExternalSet(
			BitExternalSet<T> other) {
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
	BitExternalSet(
			ExternalSetCache<T> cache,
			T element) {
		this.cache = cache;
		int pos = cache.indexOfOrAdd(element);
		bits = new long[1 + (pos >> 6)];
		bits[pos >> 6] |= 1L << (pos % 64);
	}

	/**
	 * Builds a set containing all the elements of the given iterable and
	 * connected to the given cache.
	 * 
	 * @param cache    the cache that must be used for this set
	 * @param elements the elements put inside the set
	 */
	BitExternalSet(
			ExternalSetCache<T> cache,
			Iterable<T> elements) {
		this(cache);
		for (T e : elements)
			add(e);
	}

	@Override
	public ExternalSetCache<T> getCache() {
		return cache;
	}

	private void expand(
			int targetLength) {
		long[] localbits = bits;
		bits = new long[targetLength];
		System.arraycopy(localbits, 0, bits, 0, localbits.length);
	}

	private void shrink(
			int targetLength) {
		long[] localbits = bits;
		bits = new long[targetLength];
		System.arraycopy(localbits, 0, bits, 0, targetLength);
	}

	@Override
	public boolean add(
			T e) {
		long[] localbits = bits;
		int pos = cache.indexOfOrAdd(e);
		int bitvector = pos >> 6;

		if (bitvector >= localbits.length) {
			// the array is not long enough for the position
			// of the element that we are adding
			expand(1 + bitvector);
			bits[pos >> 6] |= 1L << (pos % 64);
			return true;
		} else if ((localbits[pos >> 6] & 1L << (pos % 64)) != 0L)
			return false;

		localbits[pos >> 6] |= 1L << (pos % 64);
		return true;
	}

	@Override
	public void addAll(
			ExternalSet<T> other) {
		if (this == other)
			return;
		if (other == null)
			return;
		if (cache != other.getCache())
			return;

		if (!(other instanceof BitExternalSet))
			ExternalSet.super.addAll(other);
		else {
			BitExternalSet<T> o = (BitExternalSet<T>) other;
			long[] localbits = this.bits, otherbits = o.bits;
			int thislength = localbits.length, otherlength = otherbits.length;

			if (thislength < otherlength)
				expand(otherlength);

			for (--otherlength; otherlength >= 0; otherlength--)
				bits[otherlength] |= otherbits[otherlength];
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(
			Object e) {
		int pos;
		try {
			pos = cache.indexOf((T) e);
		} catch (ClassCastException ex) {
			// ugly, but java and generics :/
			return false;
		}

		if (pos < 0)
			return false;

		long[] localbits = this.bits;
		if (pos >> 6 >= localbits.length)
			return false;

		localbits[pos >> 6] &= ~(1L << (pos % 64));
		removeTrailingZeros();
		return true;
	}

	@Override
	public int size() {
		long bitvector;
		int count = 0;
		long[] localbits = this.bits;

		for (int pos = localbits.length - 1; pos >= 0; pos--) {
			bitvector = localbits[pos];
			if (bitvector != 0L)
				for (int i = 0; i < 64; i++) {
					int n = 64 * pos + i;
					if ((localbits[n >> 6] & 1L << (n % 64)) != 0L)
						count++;
				}
		}

		return count;
	}

	@Override
	public boolean isEmpty() {
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

		if (length != localbits.length)
			// if we decreased at least once, we can shrink the bits
			shrink(length);
	}

	@Override
	public Iterator<T> iterator() {
		return new BitSetIterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (cache == null ? 1 : cache.hashCode());
		result = prime * result + Arrays.hashCode(bits);
		return result;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean equals(
			Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;

		// custom equals for collections
		if (!(obj instanceof Set))
			return false;
		if (!(obj instanceof ExternalSet))
			// set.equals() will check for elements
			return obj.equals(this);

		ExternalSet other = (ExternalSet) obj;
		if (cache != other.getCache() || !(other instanceof BitExternalSet)) {
			// we make them have the same cache and be backed by the a bitset
			BitExternalSet o = (BitExternalSet) cache.mkSet(other);
			return Arrays.equals(bits, o.bits);
		} else
			return Arrays.equals(bits, ((BitExternalSet) other).bits);
	}

	@Override
	public String toString() {
		return "[" + StringUtils.join(this, ", ") + "]";
	}

	@Override
	public void clear() {
		this.bits = new long[1];
	}

	@Override
	public BitExternalSet<T> copy() {
		return new BitExternalSet<>(this);
	}

	@Override
	public boolean contains(
			ExternalSet<T> other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (cache != other.getCache())
			return false;

		if (!(other instanceof BitExternalSet))
			return ExternalSet.super.contains(other);

		BitExternalSet<T> o = (BitExternalSet<T>) other;
		long[] otherbits = o.bits, localbits = bits;
		if (otherbits.length > localbits.length)
			// clever check: this cannot contain other if other is bigger
			return false;

		// if at least one bit that is 0 in this is 1 in other, than this does
		// not contain other
		for (int i = otherbits.length - 1; i >= 0; i--)
			if ((localbits[i] | otherbits[i]) != localbits[i])
				return false;

		return true;
	}

	@Override
	public boolean intersects(
			ExternalSet<T> other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (cache != other.getCache())
			return false;

		if (!(other instanceof BitExternalSet))
			return ExternalSet.super.intersects(other);

		BitExternalSet<T> o = (BitExternalSet<T>) other;
		// if at least one bit is one on both, then the two intersect
		long[] otherbits = o.bits, localbits = bits;
		int min = (otherbits.length > localbits.length) ? localbits.length : otherbits.length;
		for (int i = min - 1; i >= 0; i--)
			if ((localbits[i] & otherbits[i]) != 0L)
				return true;

		return false;
	}

	@Override
	public ExternalSet<T> intersection(
			ExternalSet<T> other) {
		if (this == other)
			return this;
		if (other == null)
			return this;
		if (cache != other.getCache())
			return this;

		if (!(other instanceof BitExternalSet))
			return ExternalSet.super.intersection(other);

		BitExternalSet<T> o = (BitExternalSet<T>) other;

		int index;
		long[] otherbits;
		BitExternalSet<T> result;

		// copy the shortest one, then perform bitwise and with the longest one
		// on
		// common bits
		if (this.bits.length > o.bits.length) {
			otherbits = this.bits;
			index = o.bits.length;
			result = new BitExternalSet<>(o);
		} else {
			otherbits = o.bits;
			index = bits.length;
			result = new BitExternalSet<>(this);
		}

		long[] res = result.bits;
		while (index > 0)
			res[--index] &= otherbits[index];
		result.removeTrailingZeros();
		return result;
	}

	@Override
	public ExternalSet<T> difference(
			ExternalSet<T> other) {
		if (this == other)
			return this;
		if (other == null)
			return this;
		if (cache != other.getCache())
			return this;

		if (!(other instanceof BitExternalSet))
			return ExternalSet.super.difference(other);

		BitExternalSet<T> o = (BitExternalSet<T>) other;

		long[] localbits = this.bits;
		int pos = localbits.length;
		BitExternalSet<T> result = new BitExternalSet<>(this);
		long[] otherbits = o.bits, res = result.bits;

		if (otherbits.length < pos)
			pos = otherbits.length;

		while (--pos >= 0)
			res[pos] &= ~otherbits[pos];

		result.removeTrailingZeros();
		return result;
	}

	@Override
	public ExternalSet<T> union(
			ExternalSet<T> other) {
		if (this == other)
			return this;
		if (other == null)
			return this;
		if (cache != other.getCache())
			return this;

		if (!(other instanceof BitExternalSet))
			return ExternalSet.super.union(other);

		BitExternalSet<T> o = (BitExternalSet<T>) other;

		long[] localbits = this.bits, otherbits = o.bits;
		int thislength = localbits.length, otherlength = otherbits.length;
		BitExternalSet<T> result;

		if (thislength < otherlength) {
			result = new BitExternalSet<>(o);
			long[] res = result.bits;
			for (--thislength; thislength >= 0; thislength--)
				res[thislength] |= localbits[thislength];
		} else {
			result = new BitExternalSet<>(this);
			long[] res = result.bits;
			while (--otherlength >= 0)
				res[otherlength] |= otherbits[otherlength];
		}

		return result;
	}

	/**
	 * An iterator over the elements of a {@link BitExternalSet}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	private final class BitSetIterator
			implements
			Iterator<T> {

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
			this.bits = BitExternalSet.this.bits;
			// we go back to the integer representation
			this.totalBits = bits.length << 6;
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
				int pos = start >> 6;
				long bitvector = localbits[pos];

				while (bitvector == 0L) {
					// we can skip to the next vector
					if ((start += 64) >= l)
						return -1;
					bitvector = localbits[++pos];
				}

				long bit = 1L << (start % 64);
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
			if (next < 0)
				throw new NoSuchElementException();
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

	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(
			Object o) {
		int pos;
		try {
			pos = cache.indexOf((T) o);
		} catch (ClassCastException e) {
			// ugly, but java and generics :/
			return false;
		}

		if (pos < 0)
			// if it's not inside the cache, it's not in the set
			return false;

		int bitvector = pos >> 6;
		return bitvector < bits.length && (bits[pos >> 6] & 1L << (pos % 64)) != 0L;
	}

	@Override
	public Object[] toArray() {
		Object[] array = new Object[size()];
		int i = 0;
		for (T t : this)
			array[i++] = t;
		return array;
	}

	@Override
	public <E> E[] toArray(
			E[] a) {
		return new ArrayList<>(this).toArray(a);
	}

	@Override
	public boolean containsAll(
			Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	@Override
	public boolean addAll(
			Collection<? extends T> c) {
		boolean result = false;
		for (T o : c)
			result |= add(o);
		return result;
	}

	@Override
	public boolean retainAll(
			Collection<?> c) {
		Collection<T> toRemove = new ArrayList<>();
		for (T o : this)
			if (!c.contains(o))
				toRemove.add(o);

		for (T o : toRemove)
			remove(o);
		return !toRemove.isEmpty();
	}

	@Override
	public boolean removeAll(
			Collection<?> c) {
		Collection<T> toRemove = new ArrayList<>();
		for (T o : this)
			if (c.contains(o))
				toRemove.add(o);

		for (T o : toRemove)
			remove(o);
		return !toRemove.isEmpty();
	}

}
