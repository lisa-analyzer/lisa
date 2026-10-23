package it.unive.lisa.witness.v2;

/**
 * A checked {@link Exception} thrown by {@link WitnessReader} when a witness
 * cannot be read. This happens when the text is not valid YAML, when it does
 * not follow the witness format, or when the objects it describes break one of
 * the rules of the format. The message reports the line where the problem was
 * found, if known, and the cause is the error raised while reading.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class WitnessReadException
		extends
		Exception {

	/**
	 * The version of the serialized form of this class.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Builds the exception.
	 * 
	 * @param message the message of this exception
	 * @param cause   the cause of this exception
	 */
	public WitnessReadException(
			String message,
			Throwable cause) {
		super(message, cause);
	}
}
