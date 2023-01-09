package it.unive.lisa.util.numeric;

public class MathNumberConversionException extends Exception {

	public MathNumberConversionException(MathNumber m) {
		super("Cannot convert " + m + " to numerical value");
	}
}
