package it.unive.lisa.witness.v2;

/**
 * An {@link IllegalArgumentException} thrown when a witness object would break
 * one of the rules of the witness format.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class InvalidWitnessException
		extends
		IllegalArgumentException {

	/**
	 * The version of the serialized form of this class.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception, describing the rule that is
	 *                    broken
	 */
	public InvalidWitnessException(
			String message) {
		super(message);
	}
}
