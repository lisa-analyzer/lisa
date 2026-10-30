package it.unive.lisa.checks.verification;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The result of the verification of a candidate {@link Violation}, as computed
 * by a {@link WarningVerifier}. It is made of a {@link VerificationOutcome}, an
 * optional explanation of how the outcome has been reached (or, for
 * {@link VerificationOutcome#POSSIBLE} outcomes, of why the verification could
 * not conclude), and, for {@link VerificationOutcome#CONFIRMED} outcomes, the
 * {@link ReproducingInput}s that make the violation happen, if known.
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

	private final List<ReproducingInput> reproducingInputs;

	/**
	 * Builds the result, with no reproducing inputs.
	 *
	 * @param outcome     the outcome of the verification
	 * @param explanation the explanation of the outcome (can be {@code null})
	 */
	public VerificationResult(
			VerificationOutcome outcome,
			String explanation) {
		this(outcome, explanation, null);
	}

	/**
	 * Builds the result.
	 *
	 * @param outcome           the outcome of the verification
	 * @param explanation       the explanation of the outcome (can be
	 *                              {@code null})
	 * @param reproducingInputs the inputs reproducing the violation (can be
	 *                              {@code null} if no inputs are known, and
	 *                              must be {@code null} or empty if the outcome
	 *                              is not
	 *                              {@link VerificationOutcome#CONFIRMED})
	 */
	public VerificationResult(
			VerificationOutcome outcome,
			String explanation,
			List<ReproducingInput> reproducingInputs) {
		Objects.requireNonNull(outcome);
		if (outcome != VerificationOutcome.CONFIRMED && reproducingInputs != null && !reproducingInputs.isEmpty())
			throw new IllegalArgumentException("Only confirmed violations can have reproducing inputs");
		this.outcome = outcome;
		this.explanation = explanation;
		this.reproducingInputs = reproducingInputs == null || reproducingInputs.isEmpty()
				? Collections.emptyList()
				: Collections.unmodifiableList(List.copyOf(reproducingInputs));
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
	 * Yields the inputs reproducing the violation, if known. The returned list
	 * can be non-empty only if the outcome of this result is
	 * {@link VerificationOutcome#CONFIRMED}.
	 *
	 * @return the inputs (empty if none are known)
	 */
	public List<ReproducingInput> getReproducingInputs() {
		return reproducingInputs;
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
	 * and its explanation and reproducing inputs are the ones of the result
	 * determining the outcome. If both results determine it, their explanations
	 * are joined, and the reproducing inputs of {@code this} result are kept
	 * (or the ones of {@code other}, if {@code this} has none).
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
		if (other.outcome != lub)
			return this;

		String expl;
		if (explanation == null || other.explanation == null || explanation.equals(other.explanation))
			expl = explanation == null ? other.explanation : explanation;
		else
			expl = explanation + "\n" + other.explanation;
		List<ReproducingInput> inputs = reproducingInputs.isEmpty() ? other.reproducingInputs : reproducingInputs;

		if (Objects.equals(expl, explanation) && inputs == reproducingInputs)
			return this;
		if (Objects.equals(expl, other.explanation) && inputs == other.reproducingInputs)
			return other;
		return new VerificationResult(lub, expl, inputs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(explanation, outcome, reproducingInputs);
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
		return Objects.equals(explanation, other.explanation)
				&& outcome == other.outcome
				&& Objects.equals(reproducingInputs, other.reproducingInputs);
	}

	@Override
	public String toString() {
		return outcome
				+ (explanation == null ? "" : " (" + explanation + ")")
				+ (reproducingInputs.isEmpty() ? "" : " " + reproducingInputs);
	}
}
