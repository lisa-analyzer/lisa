package it.unive.lisa.util.numeric;

/**
 * An exception throw when someone tries to iterate over a non-finite
 * {@link IntInterval}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class InfiniteIterationException
		extends
		RuntimeException {

	private static final long serialVersionUID = -8505901233292219893L;

	/**
	 * Builds the exception.
	 * 
	 * @param i the non-finite interval on which some iterates
	 */
	public InfiniteIterationException(
			IntInterval i) {
		super(
				"Cannot iterate over the interval " + i);
	}

}
