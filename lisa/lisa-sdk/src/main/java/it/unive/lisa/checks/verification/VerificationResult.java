package it.unive.lisa.checks.verification;

import it.unive.lisa.program.cfg.statement.Statement;
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

	private final Statement thrower;

	private final VerificationResult candidate;

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
		this.thrower = null;
		this.candidate = null;
	}

	private VerificationResult(
			VerificationResult result,
			Statement thrower,
			VerificationResult candidate) {
		this.outcome = result.outcome;
		this.explanation = result.explanation;
		this.reproducingInputs = result.reproducingInputs;
		this.thrower = thrower;
		this.candidate = candidate;
	}

	/**
	 * Yields a copy of this result recording the statement raising the error of
	 * a confirmed error violation (see {@link #getThrower()}), or, for a
	 * possible result, of its candidate confirmation (see
	 * {@link #getCandidate()}). This result is returned if it is (or its
	 * candidate is) not confirmed, or if it already records one.
	 *
	 * @param thrower the statement raising the error
	 *
	 * @return the result
	 */
	public VerificationResult withThrower(
			Statement thrower) {
		if (outcome == VerificationOutcome.POSSIBLE && candidate != null) {
			VerificationResult withThrower = candidate.withThrower(thrower);
			return withThrower == candidate ? this : new VerificationResult(this, this.thrower, withThrower);
		}
		if (outcome != VerificationOutcome.CONFIRMED || this.thrower != null || thrower == null)
			return this;
		return new VerificationResult(this, thrower, candidate);
	}

	/**
	 * Yields a copy of this possible result recording a candidate confirmation
	 * (see {@link #getCandidate()}). This result is returned if it is not
	 * possible, if it already records one, or if the given one is not
	 * confirmed.
	 *
	 * @param candidate the candidate confirmation
	 *
	 * @return the result
	 */
	public VerificationResult withCandidate(
			VerificationResult candidate) {
		if (outcome != VerificationOutcome.POSSIBLE || this.candidate != null || candidate == null
				|| !candidate.isConfirmed())
			return this;
		return new VerificationResult(this, thrower, candidate);
	}

	/**
	 * Yields the candidate confirmation of a possible result, if any: the
	 * confirmed result (with its reproducing inputs) that the violation would
	 * have if the inputs of a counterexample that the verifier could not
	 * confirm were shown to reproduce it (e.g., by executing the program with
	 * them). This is not part of the equality of results.
	 *
	 * @return the candidate, or {@code null} if there is none
	 */
	public VerificationResult getCandidate() {
		return candidate;
	}

	/**
	 * Yields the statement raising the error that confirms an error violation,
	 * if known: errors reported on a statement might be raised by several
	 * statements, and a confirmation tells which one raises it in the
	 * reproduced execution. This is not part of the equality of results.
	 *
	 * @return the statement, or {@code null} if unknown
	 */
	public Statement getThrower() {
		return thrower;
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
	 * (or the ones of {@code other}, if {@code this} has none), and so are the
	 * thrower and the candidate.
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

		Statement thr = thrower == null ? other.thrower : thrower;
		VerificationResult cand = candidate == null ? other.candidate : candidate;
		if (Objects.equals(expl, explanation) && inputs == reproducingInputs && thr == thrower && cand == candidate)
			return this;
		if (Objects.equals(expl, other.explanation) && inputs == other.reproducingInputs && thr == other.thrower
				&& cand == other.candidate)
			return other;
		return new VerificationResult(lub, expl, inputs).withThrower(thr).withCandidate(cand);
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
