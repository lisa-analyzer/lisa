package it.unive.lisa.checks.verification;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.outputs.messages.StatementMessage;
import java.util.Collection;
import java.util.Map;

/**
 * An algorithm that, given the results of the analysis, verifies the
 * {@link Violation}s reported by the warnings issued by {@link SemanticCheck}s,
 * either confirming them, refuting them, or deeming them possible (see
 * {@link VerificationOutcome}). Checks issue possible warnings, attaching the
 * violation they report (see
 * {@link SemanticTool#warnOn(it.unive.lisa.program.cfg.statement.Statement, it.unive.lisa.interprocedural.ScopeId, Violation, String)}):
 * once all checks have been executed, LiSA passes all the warnings carrying a
 * violation to the verifier selected through the configuration of the
 * analysis, and turns confirmed warnings into definite ones (with their
 * reproducing inputs) and refuted warnings into refuted ones.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public interface WarningVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

	/**
	 * Verifies the violations reported by the given warnings, given the
	 * results of the analysis contained in the given tool. Verifiers are free
	 * to verify only some of the warnings (e.g., to bound the verification
	 * time): warnings that are not verified are left possible.
	 *
	 * @param tool     the tool containing the results of the analysis
	 * @param warnings the warnings to verify, each carrying the violation it
	 *                     reports ({@link StatementMessage#getViolation()})
	 *
	 * @return the result of the verification of each verified warning
	 *
	 * @throws SemanticException if something goes wrong during the verification
	 */
	Map<StatementMessage, VerificationResult> verify(
			SemanticTool<A, D> tool,
			Collection<StatementMessage> warnings)
			throws SemanticException;

	/**
	 * Sets the time after which this verifier should stop verifying warnings
	 * (e.g., to bound the time of the whole verification): warnings that are
	 * not verified by then are left possible. The default implementation
	 * ignores the deadline.
	 *
	 * @param deadline the time, as given by {@link System#currentTimeMillis()}
	 *                     ({@link Long#MAX_VALUE} means no deadline)
	 */
	default void setDeadline(
			long deadline) {
	}
}
