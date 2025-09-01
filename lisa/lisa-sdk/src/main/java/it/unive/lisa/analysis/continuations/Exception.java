package it.unive.lisa.analysis.continuations;

import it.unive.lisa.program.cfg.statement.Statement;
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

	private final Statement thrower;

	/**
	 * Builds a new continuation for the given exception type.
	 * 
	 * @param type the exception type
	 * @param thrower the statement that threw the exception
	 */
	public Exception(
			Type type,
			Statement thrower) {
		this.type = type;
		this.thrower = thrower;
	}

	/**
	 * Yields the exception type associated with this continuation.
	 * 
	 * @return the exception type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Yields the statement that threw the exception.
	 * 
	 * @return the statement that threw the exception
	 */
	public Statement getThrower() {
		return thrower;
	}

	/**
	 * Yields a new instance of this class with the same exception type and the given thrower.
	 * @param thrower the statement that threw the exception
	 * @return the new instance
	 */
	public Exception withThrower(Statement thrower) {
		return new Exception(type, thrower);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((thrower == null) ? 0 : thrower.hashCode());
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
		if (thrower == null) {
			if (other.thrower != null)
				return false;
		} else if (!thrower.equals(other.thrower))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation(type + " thrown by " + thrower);
	}
}
