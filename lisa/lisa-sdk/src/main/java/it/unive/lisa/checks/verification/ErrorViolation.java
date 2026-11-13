package it.unive.lisa.checks.verification;

import it.unive.lisa.type.Type;
import java.util.Objects;

/**
 * A {@link Violation} that happens whenever the post-state of the statement on
 * which the warning is issued contains an error continuation (i.e., an error
 * raised by the statement, or propagated to it without being caught),
 * optionally only for errors whose type is a subtype of a given one. Instances
 * of this class can be obtained through {@link Violation#error()} and
 * {@link Violation#error(Type)}.
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

	private final Type confirmed;

	/**
	 * Builds the violation.
	 *
	 * @param type the type of the errors causing the violation, or {@code null}
	 *                 to match errors of any type
	 */
	ErrorViolation(
			Type type) {
		this(type, null);
	}

	/**
	 * Builds the violation.
	 *
	 * @param type      the type of the errors that might cause the violation,
	 *                      or {@code null} to match errors of any type
	 * @param confirmed the type of the errors that certainly cause the
	 *                      violation, or {@code null} if it is the same as
	 *                      {@code type}
	 */
	ErrorViolation(
			Type type,
			Type confirmed) {
		this.type = type;
		this.confirmed = confirmed;
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

	/**
	 * Yields the type of the errors that certainly cause the violation, if it
	 * is narrower than the one of the errors that might cause it (see
	 * {@link #confirms(Type)}).
	 *
	 * @return the type, or {@code null} if it is the same as {@link #getType()}
	 */
	public Type getConfirmedType() {
		return confirmed;
	}

	/**
	 * Yields {@code true} if errors of the given type certainly cause this
	 * violation, so that the violation can be confirmed by them. Errors that
	 * match the violation ({@link #matches(Type)}) might not confirm it: for
	 * instance, an error whose type is known only through a supertype of the
	 * ones causing the violation might cause it, so that it must be considered
	 * when refuting the violation, but cannot confirm it.
	 *
	 * @param errorType the type of the error
	 *
	 * @return whether or not errors of the given type confirm this violation
	 */
	public boolean confirms(
			Type errorType) {
		return confirmed == null ? matches(errorType) : errorType.canBeAssignedTo(confirmed);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, confirmed);
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
		return Objects.equals(type, other.type) && Objects.equals(confirmed, other.confirmed);
	}

	@Override
	public String toString() {
		return "error" + (type == null ? "" : " of type " + type);
	}
}
