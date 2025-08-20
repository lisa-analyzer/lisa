package it.unive.lisa.analysis.continuations;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A {@link Continuation} that represents an erroneous state caused by one or
 * more exceptions of a specific type being raised.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Exception
		extends
		Continuation {

	private final Type type;

	/**
	 * Builds a new continuation for the given exception type.
	 * 
	 * @param type the exception type
	 */
	public Exception(
			Type type) {
		this.type = type;
	}

	/**
	 * Yields the exception type associated with this continuation.
	 * 
	 * @return the exception type
	 */
	public Type getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Exception other = (Exception) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(type.toString());
	}
}
