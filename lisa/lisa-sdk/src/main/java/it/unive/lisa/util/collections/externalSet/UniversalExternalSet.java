package it.unive.lisa.util.collections.externalSet;

import java.util.Collection;
import java.util.Iterator;

/**
 * An {@link ExternalSet} that always stays up-to-date with the contents of the
 * underlying factory, but that cannot be modified.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of elements inside this set
 */
public final class UniversalExternalSet<T>
		implements
		ExternalSet<T> {

	private static final String CANNOT_PERFORM_ERROR = "Cannot remove elements from a universal view of an external set";

	/**
	 * The cache that generated this set and that contains the elements of this
	 * set.
	 */
	private final ExternalSetCache<T> cache;

	/**
	 * Builds a set connected to the given cache.
	 * 
	 * @param cache the cache
	 */
	UniversalExternalSet(
			ExternalSetCache<T> cache) {
		this.cache = cache;
	}

	@Override
	public int size() {
		return cache.size();
	}

	@Override
	public boolean isEmpty() {
		return cache.size() == 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean contains(
			Object o) {
		try {
			return cache.indexOf((T) o) != -1;
		} catch (ClassCastException e) {
			// ugly, but java and generics :/
			return false;
		}
	}

	@Override
	public Iterator<T> iterator() {
		return cache.getAllElements().iterator();
	}

	@Override
	public Object[] toArray() {
		return cache.getAllElements().toArray();
	}

	@Override
	public <E> E[] toArray(
			E[] a) {
		return cache.getAllElements().toArray(a);
	}

	@Override
	public boolean add(
			T e) {
		throw new UnsupportedOperationException(String.format(CANNOT_PERFORM_ERROR, "add"));
	}

	@Override
	public boolean remove(
			Object o) {
		throw new UnsupportedOperationException(String.format(CANNOT_PERFORM_ERROR, "remove"));
	}

	@Override
	public boolean containsAll(
			Collection<?> c) {
		return cache.getAllElements().containsAll(c);
	}

	@Override
	public boolean addAll(
			Collection<? extends T> c) {
		throw new UnsupportedOperationException(String.format(CANNOT_PERFORM_ERROR, "add"));
	}

	@Override
	public boolean retainAll(
			Collection<?> c) {
		throw new UnsupportedOperationException(String.format(CANNOT_PERFORM_ERROR, "remove"));
	}

	@Override
	public boolean removeAll(
			Collection<?> c) {
		throw new UnsupportedOperationException(String.format(CANNOT_PERFORM_ERROR, "remove"));
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(String.format(CANNOT_PERFORM_ERROR, "remove"));

	}

	@Override
	public ExternalSetCache<T> getCache() {
		return cache;
	}

	/**
	 * Copying an {@link UniversalExternalSet} yields a {@link BitExternalSet}
	 * representing a snapshot of the actual state of the cache.<br>
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public ExternalSet<T> copy() {
		return new BitExternalSet<>(cache, this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cache == null) ? 0 : cache.hashCode());
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
			// custom equals for collections
			return copy().equals(obj);
		UniversalExternalSet<?> other = (UniversalExternalSet<?>) obj;
		if (cache == null) {
			if (other.cache != null)
				return false;
		} else if (!cache.equals(other.cache))
			return false;
		return true;
	}

}
