package it.unive.lisa.triage;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.WarningVerifier;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.triage.encoding.ExpressionEncoderFactory;
import it.unive.lisa.triage.pdr.PdrVerifier;
import it.unive.lisa.triage.reachability.ReachabilityVerifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * not verified because the verification stopped stay possible, and their
 * result explains it ({@link #NOT_VERIFIED}). Warnings that stay possible after
 * being verified keep the explanation of the last verifier that verified them.
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

	private final IndividualWarningVerifier<A, D> cheap;

	private final IndividualWarningVerifier<A, D> expensive;

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

	@Override
	public Map<StatementMessage, VerificationResult> verify(
			SemanticTool<A, D> tool,
			Collection<StatementMessage> warnings)
			throws SemanticException {
		Map<StatementMessage, VerificationResult> results = new LinkedHashMap<>();
		if (phase(cheap, tool, warnings, results))
			return results;
		phase(expensive, tool, warnings, results);
		return results;
	}

	/**
	 * Verifies, with the given verifier, the warnings that have been neither
	 * refuted nor confirmed yet, recording the results in the given map. If a
	 * warning is confirmed, the verification stops, and the warnings that
	 * have not been verified in this phase are recorded as
	 * {@link #NOT_VERIFIED}, unless they have been refuted before.
	 *
	 * @return {@code true} if a warning has been confirmed
	 */
	private static <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> boolean phase(
					IndividualWarningVerifier<A, D> verifier,
					SemanticTool<A, D> tool,
					Collection<StatementMessage> warnings,
					Map<StatementMessage, VerificationResult> results)
					throws SemanticException {
		verifier.init(tool);
		boolean confirmed = false;
		for (StatementMessage warning : warnings) {
			VerificationResult previous = results.get(warning);
			if (previous != null && previous.getOutcome() != VerificationOutcome.POSSIBLE)
				// refuted in a previous phase
				continue;
			if (confirmed) {
				results.put(warning, new VerificationResult(VerificationOutcome.POSSIBLE, NOT_VERIFIED));
				continue;
			}
			VerificationResult result = verifier.verify(tool, warning);
			results.put(warning, result);
			if (result.getOutcome() == VerificationOutcome.CONFIRMED)
				confirmed = true;
		}
		return confirmed;
	}
}
