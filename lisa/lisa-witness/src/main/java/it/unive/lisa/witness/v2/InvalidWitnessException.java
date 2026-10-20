package it.unive.lisa.witness.v2;

public class InvalidWitnessException
		extends
		IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	public InvalidWitnessException(
			String message) {
		super(message);
	}
}
