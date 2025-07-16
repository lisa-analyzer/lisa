package it.unive.lisa.util.numeric;

/**
 * An exception thrown when a {@link MathNumber} fails to be converted to a
 * specific Java numerical type.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class MathNumberConversionException
		extends
		Exception {

	private static final long serialVersionUID = 8946152199095876219L;

	/**
	 * Builds the exception.
	 * 
	 * @param m the math number that fails to be converted
	 */
	public MathNumberConversionException(
			MathNumber m) {
		super(
				"Cannot convert " + m + " to numerical value");
	}

}
