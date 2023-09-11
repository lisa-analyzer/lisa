package it.unive.lisa.util.representation;

public interface StructuredObject {

	/**
	 * Yields a {@link StructuredRepresentation} of the information contained in
	 * this object's instance.
	 * 
	 * @return the representation
	 */
	StructuredRepresentation representation();
}
