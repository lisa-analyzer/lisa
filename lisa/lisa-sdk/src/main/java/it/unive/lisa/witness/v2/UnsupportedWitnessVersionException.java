package it.unive.lisa.witness.v2;

/**
 * An {@link InvalidWitnessException} thrown when a witness has a version of the
 * witness format that is not supported.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class UnsupportedWitnessVersionException
		extends
		InvalidWitnessException {

	/**
	 * The version of the serialized form of this class.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception, with the version of the
	 *                    witness and the supported ones
	 */
	public UnsupportedWitnessVersionException(
			String message) {
		super(message);
	}
}
