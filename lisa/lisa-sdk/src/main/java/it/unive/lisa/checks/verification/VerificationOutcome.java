package it.unive.lisa.checks.verification;

/**
 * The outcome of the verification of a candidate {@link Violation}, as computed
 * by a {@link WarningVerifier}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public enum VerificationOutcome {

	/**
	 * The violation cannot happen in any execution: the warning is a false
	 * positive and should not be issued.
	 */
	REFUTED,

	/**
	 * The violation might happen, but it could not be proven nor refuted.
	 */
	POSSIBLE,

	/**
	 * The violation happens in at least one execution.
	 */
	CONFIRMED;

	/**
	 * Combines {@code this} outcome with {@code other}, where the two have been
	 * computed on disjoint sets of executions (e.g., different contexts of the
	 * same cfg). The result is {@link #CONFIRMED} if either one is, and
	 * {@link #REFUTED} only if both are.
	 *
	 * @param other the other outcome
	 *
	 * @return the combined outcome
	 */
	public VerificationOutcome lub(
			VerificationOutcome other) {
		if (this == CONFIRMED || other == CONFIRMED)
			return CONFIRMED;
		if (this == REFUTED && other == REFUTED)
			return REFUTED;
		return POSSIBLE;
	}
}
