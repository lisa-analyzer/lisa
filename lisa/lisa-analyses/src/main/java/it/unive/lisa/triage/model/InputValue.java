package it.unive.lisa.triage.model;

import it.unive.lisa.triage.inputs.InputSource;

/**
 * A {@link FreshValue} produced by a source of nondeterminism of the program
 * (an {@link InputSource}): its concrete value is part of the inputs of an
 * execution.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InputValue
		extends
		FreshValue {

	private final InputSource source;

	/**
	 * Builds the value.
	 *
	 * @param source the source producing the value
	 * @param index  a number that uniquely identifies the value among the
	 *                   fresh values of a model
	 */
	public InputValue(
			InputSource source,
			long index) {
		super(source.getType(), "$input#" + index, source.getLocation());
		this.source = source;
	}

	/**
	 * Yields the source producing this value.
	 *
	 * @return the source
	 */
	public InputSource getSource() {
		return source;
	}
}
