package it.unive.lisa.analysis.representation;

import it.unive.lisa.outputs.serializableGraph.SerializableString;
import it.unive.lisa.outputs.serializableGraph.SerializableValue;

/**
 * A {@link DomainRepresentation} in the form of a single string element.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringRepresentation extends DomainRepresentation {

	protected final String representation;

	/**
	 * Builds a new representation containing the given string.
	 * 
	 * @param representation the string
	 */
	public StringRepresentation(String representation) {
		this.representation = representation;
	}

	/**
	 * Builds a new representation starting from the given object.
	 * {@link String#valueOf(Object)} is used to extract its string
	 * representation.
	 * 
	 * @param obj the object
	 */
	public StringRepresentation(Object obj) {
		this(String.valueOf(obj));
	}

	@Override
	public SerializableValue toSerializableValue() {
		return new SerializableString(getProps(), String.valueOf(representation));
	}

	@Override
	public String toString() {
		return String.valueOf(representation);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((representation == null) ? 0 : representation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringRepresentation other = (StringRepresentation) obj;
		if (representation == null) {
			if (other.representation != null)
				return false;
		} else if (!representation.equals(other.representation))
			return false;
		return true;
	}
}
