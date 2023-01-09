package it.unive.lisa.util.numeric;

import java.util.Iterator;

public class IntIntervalIterator implements Iterator<Long> {

	private long init;
	private final long end;
	
	public IntIntervalIterator(long init, long end) {
		this.init = init;
		this.end = end;
	}
	
	@Override
	public boolean hasNext() {
		return init < end;
	}

	@Override
	public Long next() {
		return init++;
	}

}
