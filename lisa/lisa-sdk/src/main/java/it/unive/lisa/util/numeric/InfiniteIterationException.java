package it.unive.lisa.util.numeric;

public class InfiniteIterationException extends RuntimeException {

	public InfiniteIterationException(IntInterval i) {
		super("Cannot iterate over the interval " + i);
	}
}
