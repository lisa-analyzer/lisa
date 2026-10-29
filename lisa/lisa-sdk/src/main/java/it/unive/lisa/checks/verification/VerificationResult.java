package it.unive.lisa.checks.verification;

import java.util.Objects;

/**
 * The result of the verification of a candidate {@link Violation}, as computed
 * by a {@link WarningVerifier}. It is made of a {@link VerificationOutcome} and
 * an optional explanation of how the outcome has been reached (or, for
 * {@link VerificationOutcome#POSSIBLE} outcomes, of why the verification could
 * not conclude).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class VerificationResult {

	/**
	 * A {@link VerificationOutcome#REFUTED} result with no explanation.
	 */
	public static final VerificationResult REFUTED = new VerificationResult(VerificationOutcome.REFUTED, null);

	/**
	 * A {@link VerificationOutcome#POSSIBLE} result with no explanation.
	 */
	public static final VerificationResult POSSIBLE = new VerificationResult(VerificationOutcome.POSSIBLE, null);

	/**
	 * A {@link VerificationOutcome#CONFIRMED} result with no explanation.
	 */
	public static final VerificationResult CONFIRMED = new VerificationResult(VerificationOutcome.CONFIRMED, null);

	private final VerificationOutcome outcome;

	private final String explanation;

	/**
	 * Builds the result.
	 *
	 * @param outcome     the outcome of the verification
	 * @param explanation the explanation of the outcome (can be {@code null})
	 */
	public VerificationResult(
			VerificationOutcome outcome,
			String explanation) {
		Objects.requireNonNull(outcome);
		this.outcome = outcome;
		this.explanation = explanation;
	}

	/**
	 * Yields the outcome of the verification.
	 *
	 * @return the outcome
	 */
	public VerificationOutcome getOutcome() {
		return outcome;
	}

	/**
	 * Yields the explanation of the outcome, if any.
	 *
	 * @return the explanation, or {@code null}
	 */
	public String getExplanation() {
		return explanation;
	}

	/**
	 * Yields {@code true} if the outcome of this result is
	 * {@link VerificationOutcome#REFUTED}.
	 *
	 * @return whether or not the violation has been refuted
	 */
	public boolean isRefuted() {
		return outcome == VerificationOutcome.REFUTED;
	}

	/**
	 * Yields {@code true} if the outcome of this result is
	 * {@link VerificationOutcome#POSSIBLE}.
	 *
	 * @return whether or not the violation is possible
	 */
	public boolean isPossible() {
		return outcome == VerificationOutcome.POSSIBLE;
	}

	/**
	 * Yields {@code true} if the outcome of this result is
	 * {@link VerificationOutcome#CONFIRMED}.
	 *
	 * @return whether or not the violation has been confirmed
	 */
	public boolean isConfirmed() {
		return outcome == VerificationOutcome.CONFIRMED;
	}

	/**
	 * Combines {@code this} result with {@code other}, where the two have been
	 * computed on disjoint sets of executions (e.g., different contexts of the
	 * same cfg). The outcome of the returned result is the
	 * {@link VerificationOutcome#lub(VerificationOutcome)} of the two outcomes,
	 * and its explanation is the one of the result determining the outcome (if
	 * both do, explanations are joined).
	 *
	 * @param other the other result
	 *
	 * @return the combined result
	 */
	public VerificationResult lub(
			VerificationResult other) {
		VerificationOutcome lub = outcome.lub(other.outcome);
		if (outcome != lub)
			return other;
		if (other.outcome != lub || other.explanation == null || other.explanation.equals(explanation))
			return this;
		if (explanation == null)
			return other;
		return new VerificationResult(lub, explanation + "\n" + other.explanation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(explanation, outcome);
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
		VerificationResult other = (VerificationResult) obj;
		return Objects.equals(explanation, other.explanation) && outcome == other.outcome;
	}

	@Override
	public String toString() {
		return outcome + (explanation == null ? "" : " (" + explanation + ")");
	}
}
