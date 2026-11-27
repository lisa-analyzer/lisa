package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.interprocedural.ScopeId;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A message reported by LiSA on the program under analysis. Besides its text, a
 * message records the context it has been reported on (if any), the
 * {@link Violation} it reports (if any), so that it can be verified after the
 * analysis, and the {@link VerificationResult} of that verification (if it has
 * been verified). The violation is not part of the identity of the message.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Message
		implements
		Comparable<Message> {

	/**
	 * The message of this message
	 */
	private final String message;

	/**
	 * The context where this message was reported on (compared through its
	 * textual representation and its hash, as not all {@link ScopeId}s define
	 * equality)
	 */
	private final ScopeId<?> context;

	/**
	 * The violation reported by this message, if known
	 */
	private final Violation violation;

	/**
	 * The result of the verification of the violation reported by this message,
	 * if it has been verified
	 */
	private final VerificationResult verdict;

	/**
	 * Builds the message.
	 *
	 * @param message the message of this message
	 */
	public Message(
			String message) {
		this(message, null, null, null);
	}

	/**
	 * Builds the message.
	 *
	 * @param message   the message of this message
	 * @param context   the context where this message was reported on (can be
	 *                      {@code null})
	 * @param violation the violation reported by this message (can be
	 *                      {@code null} if unknown)
	 * @param verdict   the result of the verification of the violation (can be
	 *                      {@code null} if it has not been verified)
	 */
	protected Message(
			String message,
			ScopeId<?> context,
			Violation violation,
			VerificationResult verdict) {
		this.message = message;
		this.context = context;
		this.violation = violation;
		this.verdict = verdict;
	}

	/**
	 * Yields the message of this message.
	 *
	 * @return the message of this message
	 */
	public final String getMessage() {
		return message;
	}

	/**
	 * Yields the context where this message was reported on, if any (e.g., the
	 * context of the analysis result that caused the message).
	 *
	 * @return the context, or {@code null}
	 */
	public final ScopeId<?> getContext() {
		return context;
	}

	/**
	 * Yields the hash of the context where this message was reported on, if any
	 * (see {@link ScopeId#getHash()}). Different contexts might have the same
	 * textual representation (e.g., when they also depend on the abstract state
	 * at the call sites), while their hashes differ.
	 *
	 * @return the hash of the context, or {@code null}
	 */
	public final String getContextHash() {
		return context == null ? null : context.getHash();
	}

	/**
	 * Yields the violation reported by this message, if known.
	 *
	 * @return the violation, or {@code null}
	 */
	public final Violation getViolation() {
		return violation;
	}

	/**
	 * Yields the result of the verification of the violation reported by this
	 * message, if it has been verified (see {@link #getViolation()}).
	 *
	 * @return the result, or {@code null} if this message has not been verified
	 */
	public final VerificationResult getVerdict() {
		return verdict;
	}

	/**
	 * Yields the tag of this message, identifying the kind of target this
	 * message has been reported on.
	 *
	 * @return the tag of this message
	 */
	public String getTag() {
		return "GENERIC";
	}

	/**
	 * Yields the signature of the cfg this message has been reported on, or
	 * that contains the target this message has been reported on (e.g., a
	 * statement), if any.
	 *
	 * @return the signature of the cfg, or {@code null} if this message is not
	 *             related to a cfg
	 */
	public String getCFGSignature() {
		return null;
	}

	/**
	 * Yields a textual representation of the target this message has been
	 * reported on (e.g., a statement), if any. If the target is a cfg, this
	 * method returns {@code null} and its signature is returned by
	 * {@link #getCFGSignature()} instead.
	 *
	 * @return the textual representation of the target, or {@code null}
	 */
	public String getTargetText() {
		return null;
	}

	/**
	 * Yields the message of this message, prefixed with its tag.
	 *
	 * @return the tagged message
	 */
	public final String getTaggedMessage() {
		return "[" + getTag() + "] " + getMessage();
	}

	@Override
	public int compareTo(
			Message o) {
		int cmp;
		if ((cmp = StringUtils.compare(message, o.message)) != 0)
			return cmp;
		if ((cmp = StringUtils.compare(Objects.toString(context, null), Objects.toString(o.context, null))) != 0)
			return cmp;
		if ((cmp = StringUtils.compare(getContextHash(), o.getContextHash())) != 0)
			return cmp;
		return compare(verdict, o.verdict);
	}

	private static int compare(
			VerificationResult first,
			VerificationResult second) {
		if (first == second)
			return 0;
		if (first == null)
			return -1;
		if (second == null)
			return 1;
		int cmp;
		if ((cmp = first.getOutcome().compareTo(second.getOutcome())) != 0)
			return cmp;
		List<ReproducingInput> inputs = first.getReproducingInputs();
		List<ReproducingInput> others = second.getReproducingInputs();
		if ((cmp = Integer.compare(inputs.size(), others.size())) != 0)
			return cmp;
		for (int i = 0; i < inputs.size(); i++)
			if ((cmp = inputs.get(i).compareTo(others.get(i))) != 0)
				return cmp;
		return StringUtils.compare(first.getExplanation(), second.getExplanation());
	}

	@Override
	public int hashCode() {
		return Objects.hash(Objects.toString(context, null), getContextHash(), message, verdict);
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
		Message other = (Message) obj;
		return Objects.equals(Objects.toString(context, null), Objects.toString(other.context, null))
				&& Objects.equals(getContextHash(), other.getContextHash())
				&& Objects.equals(message, other.message)
				&& Objects.equals(verdict, other.verdict);
	}

	@Override
	public String toString() {
		return getTaggedMessage();
	}

}
