package it.unive.lisa.checks.verification;

import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * A {@link Violation} that happens whenever the statement on which the warning
 * is issued moves the execution to an error continuation, optionally only for
 * errors whose type is a subtype of a given one. Instances of this class can be
 * obtained through {@link Violation#error()} and {@link Violation#error(Type)}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ErrorViolation
		extends
		Violation {

	/**
	 * The instance of this class that matches errors of any type.
	 */
	static final ErrorViolation ANY = new ErrorViolation(null);

	private final Type type;

	/**
	 * Builds the violation.
	 *
	 * @param type the type of the errors causing the violation, or
	 *                 {@code null} to match errors of any type
	 */
	ErrorViolation(
			Type type) {
		this.type = type;
	}

	/**
	 * Yields the type of the errors causing the violation, if any.
	 *
	 * @return the type, or {@code null} if errors of any type cause the
	 *             violation
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Yields {@code true} if errors of the given type cause this violation.
	 *
	 * @param errorType the type of the error
	 *
	 * @return whether or not errors of the given type cause this violation
	 */
	public boolean matches(
			Type errorType) {
		return type == null || errorType.canBeAssignedTo(type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type);
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
		ErrorViolation other = (ErrorViolation) obj;
		return Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "error" + (type == null ? "" : " of type " + type);
	}
}
