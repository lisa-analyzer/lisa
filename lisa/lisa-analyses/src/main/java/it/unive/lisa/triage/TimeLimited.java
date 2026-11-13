package it.unive.lisa.triage;

/**
 * A verifier whose time can be bounded from outside, e.g., by a verifier
 * sharing a time budget among several warnings (see
 * {@link FirstViolationVerifier#withTimeBudget(long)}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface TimeLimited {

	/**
	 * Bounds the time of the verification of each of the next warnings: the
	 * verification of a warning takes at most the given time, or the time of
	 * the own budget of the verifier, if smaller.
	 *
	 * @param millis the time, in milliseconds, or {@code 0} to remove the bound
	 */
	void setTimeLimit(
			long millis);

	/**
	 * Bounds the time of the verification of each of the next warnings, also
	 * replacing the own budget of the verifier: the verification of a warning
	 * takes at most the given time. This is used to verify again, with the time
	 * left, the warnings whose verification exhausted that budget. By default,
	 * this is the same as {@link #setTimeLimit(long)}.
	 *
	 * @param millis the time, in milliseconds, or {@code 0} to remove the bound
	 */
	default void setExtendedTimeLimit(
			long millis) {
		setTimeLimit(millis);
	}
}
