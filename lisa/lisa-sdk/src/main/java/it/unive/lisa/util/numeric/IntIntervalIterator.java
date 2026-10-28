package it.unive.lisa.util.numeric;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The {@link IntInterval} iterator.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class IntIntervalIterator
		implements
		Iterator<Long> {

	private long init;

	private final long end;

	/**
	 * Builds the iterator.
	 * 
	 * @param init low bound
	 * @param end  high bound
	 */
	public IntIntervalIterator(
			long init,
			long end) {
		this.init = init;
		this.end = end;
	}

	@Override
	public boolean hasNext() {
		return init <= end;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NoSuchElementException if the iteration has no more elements,
	 *                                    that is, if {@link #hasNext()} returns
	 *                                    {@code false}
	 */
	@Override
	public Long next() {
		if (!hasNext())
			throw new NoSuchElementException();
		return init++;
	}

}
