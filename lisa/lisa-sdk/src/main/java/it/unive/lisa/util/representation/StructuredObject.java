package it.unive.lisa.util.representation;

/**
 * An object that whose contents can be converted to a domain-independent
 * {@link StructuredRepresentation} for later use.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface StructuredObject {

	/**
	 * Yields a {@link StructuredRepresentation} of the information contained in
	 * this object's instance.
	 * 
	 * @return the representation
	 */
	StructuredRepresentation representation();

}
