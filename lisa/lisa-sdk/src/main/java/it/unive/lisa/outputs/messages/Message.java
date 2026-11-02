package it.unive.lisa.outputs.messages;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.interprocedural.ScopeId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A message reported by LiSA on the program under analysis. Besides its text, a
 * message records whether it is definite (i.e., the reported behavior happens
 * in at least one execution of the program), refuted (i.e., the reported
 * behavior happens in no execution of the program), or possible, the context it
 * has been reported on (if any), and, for definite messages, the
 * {@link ReproducingInput}s that make the reported behavior happen, if known.
 * Messages can also carry the {@link Violation} they report, so that it can be
 * verified after the analysis, and the explanation of the outcome of that
 * verification: neither is part of the identity of the message.
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
	 * Whether or not this message is definite
	 */
	private final boolean definite;

	/**
	 * Whether or not this message is refuted
	 */
	private final boolean refuted;

	/**
	 * The violation reported by this message, if known
	 */
	private final Violation violation;

	/**
	 * The explanation of the outcome of the verification of the violation
	 * reported by this message, if any
	 */
	private final String explanation;

	/**
	 * The inputs reproducing the behavior reported by this message
	 */
	private final List<ReproducingInput> reproducingInputs;

	/**
	 * Builds a possible message, with no reproducing inputs.
	 *
	 * @param message the message of this message
	 */
	public Message(
			String message) {
		this(message, false, null);
	}

	/**
	 * Builds the message.
	 *
	 * @param message           the message of this message
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public Message(
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		this(message, null, definite, reproducingInputs);
	}

	/**
	 * Builds the message.
	 *
	 * @param message           the message of this message
	 * @param context           the context where this message was reported on
	 *                              (can be {@code null})
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 */
	public Message(
			String message,
			ScopeId<?> context,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		this(message, context, definite, false, reproducingInputs, null);
	}

	/**
	 * Builds the message.
	 *
	 * @param message           the message of this message
	 * @param context           the context where this message was reported on
	 *                              (can be {@code null})
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param refuted           whether or not this message is refuted, that is,
	 *                              if the reported behavior happens in no
	 *                              execution of the program (a message cannot
	 *                              be both definite and refuted)
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 * @param violation         the violation reported by this message (can be
	 *                              {@code null} if unknown)
	 * 
	 * @throws IllegalArgumentException if both {@code definite} and
	 *                                      {@code refuted} are {@code true}
	 */
	protected Message(
			String message,
			ScopeId<?> context,
			boolean definite,
			boolean refuted,
			List<ReproducingInput> reproducingInputs,
			Violation violation) {
		this(message, context, definite, refuted, reproducingInputs, violation, null);
	}

	/**
	 * Builds the message.
	 *
	 * @param message           the message of this message
	 * @param context           the context where this message was reported on
	 *                              (can be {@code null})
	 * @param definite          whether or not this message is definite, that
	 *                              is, if the reported behavior happens in at
	 *                              least one execution of the program
	 * @param refuted           whether or not this message is refuted, that is,
	 *                              if the reported behavior happens in no
	 *                              execution of the program (a message cannot
	 *                              be both definite and refuted)
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              this message (can be {@code null} if no
	 *                              inputs are known)
	 * @param violation         the violation reported by this message (can be
	 *                              {@code null} if unknown)
	 * @param explanation       the explanation of the outcome of the
	 *                              verification of the violation (can be
	 *                              {@code null} if there is none)
	 * 
	 * @throws IllegalArgumentException if both {@code definite} and
	 *                                      {@code refuted} are {@code true}
	 */
	protected Message(
			String message,
			ScopeId<?> context,
			boolean definite,
			boolean refuted,
			List<ReproducingInput> reproducingInputs,
			Violation violation,
			String explanation) {
		if (definite && refuted)
			throw new IllegalArgumentException("A message cannot be both definite and refuted");
		this.message = message;
		this.context = context;
		this.definite = definite;
		this.refuted = refuted;
		this.violation = violation;
		this.explanation = explanation;
		this.reproducingInputs = reproducingInputs == null || reproducingInputs.isEmpty()
				? Collections.emptyList()
				: Collections.unmodifiableList(List.copyOf(reproducingInputs));
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
	 * Yields whether or not this message is definite, that is, if the reported
	 * behavior happens in at least one execution of the program.
	 *
	 * @return {@code true} if this message is definite, {@code false} if it is
	 *             possible
	 */
	public final boolean isDefinite() {
		return definite;
	}

	/**
	 * Yields whether or not this message is refuted, that is, if the reported
	 * behavior happens in no execution of the program.
	 *
	 * @return {@code true} if this message is refuted
	 */
	public final boolean isRefuted() {
		return refuted;
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
	 * Yields the explanation of the outcome of the verification of the
	 * violation reported by this message, if any (e.g., why the violation
	 * could be neither confirmed nor refuted).
	 *
	 * @return the explanation, or {@code null}
	 */
	public final String getExplanation() {
		return explanation;
	}

	/**
	 * Yields the inputs reproducing the behavior reported by this message.
	 *
	 * @return the inputs (empty if none are known)
	 */
	public final List<ReproducingInput> getReproducingInputs() {
		return reproducingInputs;
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
		if ((cmp = Boolean.compare(definite, o.definite)) != 0)
			return cmp;
		if ((cmp = Boolean.compare(refuted, o.refuted)) != 0)
			return cmp;
		if ((cmp = Integer.compare(reproducingInputs.size(), o.reproducingInputs.size())) != 0)
			return cmp;
		for (int i = 0; i < reproducingInputs.size(); i++)
			if ((cmp = reproducingInputs.get(i).compareTo(o.reproducingInputs.get(i))) != 0)
				return cmp;
		return 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(Objects.toString(context, null), getContextHash(), definite, refuted, message,
				reproducingInputs);
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
		return definite == other.definite
				&& refuted == other.refuted
				&& Objects.equals(Objects.toString(context, null), Objects.toString(other.context, null))
				&& Objects.equals(getContextHash(), other.getContextHash())
				&& Objects.equals(message, other.message)
				&& Objects.equals(reproducingInputs, other.reproducingInputs);
	}

	@Override
	public String toString() {
		return getTaggedMessage();
	}

}
