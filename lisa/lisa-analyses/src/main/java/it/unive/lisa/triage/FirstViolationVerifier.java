package it.unive.lisa.triage;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.checks.verification.WarningVerifier;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.encoding.ExpressionEncoderFactory;
import it.unive.lisa.triage.pdr.PdrVerifier;
import it.unive.lisa.triage.reachability.ReachabilityVerifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link WarningVerifier} that aims at confirming one warning as quickly as
 * possible, making it definite. Warnings are verified in two phases:
 * <ol>
 * <li>all warnings are verified with a cheap verifier (by default, a
 * {@link ReachabilityVerifier}), stopping at the first warning that is
 * confirmed;</li>
 * <li>if no warning has been confirmed, the warnings that have not been refuted
 * are verified with an expensive verifier (by default, a {@link PdrVerifier}),
 * again stopping at the first warning that is confirmed.</li>
 * </ol>
 * Refutations found along the way are kept, so that a warning refuted by the
 * cheap verifier is not verified again by the expensive one. Warnings that are
 * not verified because the verification stopped stay possible, and their result
 * explains it ({@link #NOT_VERIFIED}). Warnings that stay possible after being
 * verified keep the explanation of the last verifier that verified them.<br>
 * <br>
 * If a deadline is set and time is left after the two phases, without a
 * confirmed warning, the warnings whose verification exhausted the own time
 * budget of the expensive verifier (see {@link #EXHAUSTED}) are verified again
 * by it, sharing the time left (see
 * {@link TimeLimited#setExtendedTimeLimit(long)}): the budget of the verifier
 * keeps a hard warning from taking the time of the other ones, but the time
 * that the other ones did not use goes to it.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public class FirstViolationVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		WarningVerifier<A, D> {

	/**
	 * The explanation of the warnings that are not verified, since a warning
	 * has been confirmed before them.
	 */
	public static final String NOT_VERIFIED = "not verified: the verification stopped at the first confirmed warning";

	/**
	 * The explanation of the warnings that are not verified, since the time
	 * budget of the verification ran out before them (see
	 * {@link #withTimeBudget(long)}).
	 */
	public static final String NOT_VERIFIED_IN_TIME = "not verified: the time budget of the verification ran out";

	/**
	 * The minimum time given to the verification of each warning when the time
	 * budget is shared among warnings (see {@link #withTimeBudget(long)}), in
	 * milliseconds (unless less time is left).
	 */
	public static final long MIN_SHARE_MILLIS = 10_000;

	/**
	 * The text in the explanation of a possible result telling that the
	 * verification exhausted its time budget, so that verifying the warning
	 * again with more time might decide it.
	 */
	public static final String EXHAUSTED = "time budget exhausted";

	private final IndividualWarningVerifier<A, D> cheap;

	private final IndividualWarningVerifier<A, D> expensive;

	/**
	 * The time budget of each verification of a set of warnings, in
	 * milliseconds, or {@code 0} if there is none.
	 */
	private long budget;

	/**
	 * The time by which every verification must end (see
	 * {@link #withDeadline(long)}), or {@code 0} if there is none.
	 */
	private long end;

	/**
	 * Builds the verifier, first using a {@link ReachabilityVerifier} and then
	 * a {@link PdrVerifier} with default parameters. The latter does not check
	 * warnings against the results of the analysis again
	 * ({@link PdrVerifier#withFixpointCheck(boolean)}), since the former
	 * already does.
	 */
	public FirstViolationVerifier() {
		this(ExpressionEncoderFactory.DEFAULT);
	}

	/**
	 * Builds the verifier, first using a {@link ReachabilityVerifier} and then
	 * a {@link PdrVerifier} with default parameters, as
	 * {@link #FirstViolationVerifier()}, where the latter encodes expressions
	 * with the encoders built by the given factory (see
	 * {@link PdrVerifier#withEncoder(ExpressionEncoderFactory)}).
	 *
	 * @param encoders the factory of the encoders of expressions
	 */
	public FirstViolationVerifier(
			ExpressionEncoderFactory encoders) {
		this(new ReachabilityVerifier<>(), new PdrVerifier<A, D>().withFixpointCheck(false).withEncoder(encoders));
	}

	/**
	 * Builds the verifier.
	 *
	 * @param cheap     the verifier used in the first phase
	 * @param expensive the verifier used in the second phase
	 */
	public FirstViolationVerifier(
			IndividualWarningVerifier<A, D> cheap,
			IndividualWarningVerifier<A, D> expensive) {
		this.cheap = cheap;
		this.expensive = expensive;
	}

	/**
	 * Sets the time budget of each verification of a set of warnings (default:
	 * none). The time left is shared among the warnings still to verify, when
	 * the verifier of the phase can be bounded ({@link TimeLimited}): each
	 * warning can take all the time left (within the own budget of the
	 * verifier) except {@link #MIN_SHARE_MILLIS} for each of the warnings after
	 * it, and the time it does not use goes to the next ones. Since the
	 * verification stops at the first confirmed warning, this lets the first
	 * warnings be verified as they would be without a budget, as long as there
	 * is time for the other ones. When the budget runs out, the warnings not
	 * verified yet stay possible ({@link #NOT_VERIFIED_IN_TIME}). A program
	 * might fail many checks at the same statement (e.g., in different
	 * contexts): the first warning of each statement is verified before the
	 * other ones, so that the budget is spent on different statements first.
	 *
	 * @param millis the time budget, in milliseconds, or {@code 0} for none
	 *
	 * @return this verifier
	 */
	public FirstViolationVerifier<A, D> withTimeBudget(
			long millis) {
		this.budget = millis;
		return this;
	}

	/**
	 * Sets the time by which every verification of a set of warnings must end
	 * (default: none), as {@link System#currentTimeMillis()} gives it: the time
	 * left until then is shared among the warnings, as the time budget of
	 * {@link #withTimeBudget(long)} is (if both are set, the verification ends
	 * when the first of them is reached). This bounds the verification when the
	 * whole run of the tool must end by a given time.
	 *
	 * @param time the time, or {@code 0} for none
	 *
	 * @return this verifier
	 */
	public FirstViolationVerifier<A, D> withDeadline(
			long time) {
		this.end = time;
		return this;
	}

	@Override
	public Map<StatementMessage, VerificationResult> verify(
			SemanticTool<A, D> tool,
			Collection<StatementMessage> warnings)
			throws SemanticException {
		long deadline = budget > 0 ? System.currentTimeMillis() + budget : Long.MAX_VALUE;
		if (end > 0)
			deadline = Math.min(deadline, end);
		List<StatementMessage> ordered = byStatement(warnings);
		Map<StatementMessage, VerificationResult> results = new LinkedHashMap<>();
		try {
			if (phase(cheap, tool, ordered, results, deadline))
				return results;
			if (phase(expensive, tool, ordered, results, deadline))
				return results;
			if (deadline != Long.MAX_VALUE && expensive instanceof TimeLimited)
				again(expensive, tool, ordered, results, deadline);
			return results;
		} finally {
			for (IndividualWarningVerifier<A, D> verifier : List.of(cheap, expensive))
				if (verifier instanceof TimeLimited)
					((TimeLimited) verifier).setTimeLimit(0);
		}
	}

	/**
	 * Yields the given warnings, where the first warning of each statement (and
	 * violation) comes before the other warnings of the same statement (e.g.,
	 * in other contexts), keeping their order otherwise.
	 */
	private static List<StatementMessage> byStatement(
			Collection<StatementMessage> warnings) {
		List<StatementMessage> first = new ArrayList<>();
		List<StatementMessage> others = new ArrayList<>();
		Set<Pair<Statement, Violation>> seen = new HashSet<>();
		for (StatementMessage warning : warnings)
			if (seen.add(Pair.of(warning.getStatement(), warning.getViolation())))
				first.add(warning);
			else
				others.add(warning);
		first.addAll(others);
		return first;
	}

	/**
	 * Verifies, with the given verifier, the warnings that have been neither
	 * refuted nor confirmed yet, recording the results in the given map. If a
	 * warning is confirmed, the verification stops, and the warnings that have
	 * not been verified in this phase are recorded as {@link #NOT_VERIFIED},
	 * unless they have been refuted before.
	 *
	 * @return {@code true} if a warning has been confirmed
	 */
	private static <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> boolean phase(
					IndividualWarningVerifier<A, D> verifier,
					SemanticTool<A, D> tool,
					List<StatementMessage> warnings,
					Map<StatementMessage, VerificationResult> results,
					long deadline)
					throws SemanticException {
		verifier.init(tool);
		boolean confirmed = false;
		// the warnings this phase verifies (those not refuted before)
		int left = 0;
		for (StatementMessage warning : warnings) {
			VerificationResult previous = results.get(warning);
			if (previous == null || previous.getOutcome() == VerificationOutcome.POSSIBLE)
				left++;
		}
		for (StatementMessage warning : warnings) {
			VerificationResult previous = results.get(warning);
			if (previous != null && previous.getOutcome() != VerificationOutcome.POSSIBLE)
				// refuted in a previous phase
				continue;
			if (confirmed) {
				results.put(warning, new VerificationResult(VerificationOutcome.POSSIBLE, NOT_VERIFIED));
				continue;
			}
			long now = System.currentTimeMillis();
			if (now >= deadline) {
				results.put(warning, new VerificationResult(VerificationOutcome.POSSIBLE, NOT_VERIFIED_IN_TIME));
				continue;
			}
			if (deadline != Long.MAX_VALUE && verifier instanceof TimeLimited)
				// all the time left (within the own budget of the verifier),
				// except the minimum share of each of the other warnings to
				// verify: the time a warning does not use goes to the next
				// ones, as this is computed again for each warning
				((TimeLimited) verifier).setTimeLimit(Math.min(deadline - now,
						Math.max(MIN_SHARE_MILLIS, deadline - now - MIN_SHARE_MILLIS * (left - 1))));
			left--;
			VerificationResult result = verifier.verify(tool, warning);
			results.put(warning, result);
			if (result.getOutcome() == VerificationOutcome.CONFIRMED)
				confirmed = true;
		}
		return confirmed;
	}

	/**
	 * Verifies again, sharing the time left until the deadline, the warnings
	 * whose verification exhausted the own time budget of the given verifier,
	 * stopping at the first warning that is confirmed. The time of each warning
	 * is the time left divided by the number of warnings still to verify, so
	 * that the time a warning does not use goes to the next ones. The warnings
	 * that are not verified again keep their result.
	 */
	private static <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> void again(
					IndividualWarningVerifier<A, D> verifier,
					SemanticTool<A, D> tool,
					List<StatementMessage> warnings,
					Map<StatementMessage, VerificationResult> results,
					long deadline)
					throws SemanticException {
		List<StatementMessage> exhausted = new ArrayList<>();
		for (StatementMessage warning : warnings) {
			VerificationResult previous = results.get(warning);
			if (previous != null && previous.getOutcome() == VerificationOutcome.POSSIBLE
					&& previous.getExplanation() != null && previous.getExplanation().contains(EXHAUSTED))
				exhausted.add(warning);
		}
		int left = exhausted.size();
		for (StatementMessage warning : exhausted) {
			long now = System.currentTimeMillis();
			if (deadline - now < MIN_SHARE_MILLIS)
				// not enough time for a further attempt
				return;
			((TimeLimited) verifier).setExtendedTimeLimit((deadline - now) / left--);
			VerificationResult result = verifier.verify(tool, warning);
			if (result.getOutcome() != VerificationOutcome.POSSIBLE)
				results.put(warning, result);
			if (result.getOutcome() == VerificationOutcome.CONFIRMED)
				return;
		}
	}
}
