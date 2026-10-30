package it.unive.lisa.checks.verification;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * An algorithm that, given the results of the analysis, verifies candidate
 * {@link Violation}s before {@link SemanticCheck}s issue warnings for them,
 * either confirming them, refuting them, or deeming them possible (see
 * {@link VerificationOutcome}). The verifier to use is selected through the
 * configuration of the analysis, and it is queried by checks through
 * {@link SemanticTool#verify(AnalyzedCFG, Statement, Violation)}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public interface WarningVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

	/**
	 * Initializes this verifier with the given tool, that contains the results
	 * of the analysis. A call to this method should effectively re-initialize
	 * the verifier as if it is yet to be used, discarding any information
	 * computed for previous tools. This is useful when the same instance is
	 * used in multiple analyses.
	 *
	 * @param tool the tool containing the results of the analysis
	 *
	 * @throws SemanticException if something goes wrong during the
	 *                               initialization
	 */
	void init(
			SemanticTool<A, D> tool)
			throws SemanticException;

	/**
	 * Verifies whether the given violation can happen at the given statement,
	 * considering only the executions analyzed in the context of the given
	 * result. Implementations should return
	 * {@link VerificationOutcome#POSSIBLE} for kinds of violations they do not
	 * support.
	 *
	 * @param result    the result containing the statement
	 * @param st        the statement on which the warning would be issued
	 * @param violation the violation to verify
	 *
	 * @return the result of the verification
	 *
	 * @throws SemanticException if something goes wrong during the verification
	 */
	VerificationResult verify(
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation)
			throws SemanticException;
}
