package it.unive.lisa.checks.verification;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link WarningVerifier} that verifies each warning independently of the
 * others, by verifying the violation it reports in the results of the analysis
 * of the statement it is reported on. Implementations only have to define how
 * a single violation is verified in a single result
 * ({@link #verify(AnalyzedCFG, Statement, Violation)}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public abstract class IndividualWarningVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		WarningVerifier<A, D> {

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
	public abstract void init(
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
	 * @param st        the statement on which the warning is issued
	 * @param violation the violation to verify
	 *
	 * @return the result of the verification
	 *
	 * @throws SemanticException if something goes wrong during the verification
	 */
	public abstract VerificationResult verify(
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation)
			throws SemanticException;

	/**
	 * Verifies the violation reported by the given warning, in the results of
	 * the cfg of its statement that have been computed in the context of the
	 * warning, or in all of them if the warning has no context. The results of
	 * the single verifications are combined through
	 * {@link VerificationResult#lub(VerificationResult)}: the violation is
	 * confirmed if it is confirmed in at least one of the results (and the
	 * verification stops at the first confirmation), and it is refuted only if
	 * it is refuted in all of them. This verifier must have been initialized
	 * with the given tool ({@link #init(SemanticTool)}).
	 *
	 * @param tool    the tool containing the results of the analysis
	 * @param warning the warning to verify
	 *
	 * @return the result of the verification, that is
	 *             {@link VerificationResult#POSSIBLE} if the warning does not
	 *             carry a violation or if no result can be used to verify it
	 *
	 * @throws SemanticException if something goes wrong during the verification
	 */
	public VerificationResult verify(
			SemanticTool<A, D> tool,
			StatementMessage warning)
			throws SemanticException {
		Violation violation = warning.getViolation();
		if (violation == null)
			return VerificationResult.POSSIBLE;
		Statement st = warning.getStatement();
		ScopeId<?> context = warning.getContext();
		List<AnalyzedCFG<A>> results = new ArrayList<>();
		Collection<AnalyzedCFG<A>> all = tool.getResultOf(st.getCFG());
		if (all != null)
			for (AnalyzedCFG<A> result : all)
				if (context == null
						|| Objects.equals(result.getId(), context)
						|| Objects.equals(Objects.toString(result.getId()), context.toString()))
					results.add(result);
		if (results.isEmpty())
			return VerificationResult.POSSIBLE;

		VerificationResult outcome = VerificationResult.REFUTED;
		for (AnalyzedCFG<A> result : results) {
			outcome = outcome.lub(verify(result, st, violation));
			if (outcome.getOutcome() == VerificationOutcome.CONFIRMED)
				break;
		}
		return outcome;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This verifier is initialized with the given tool, and then each warning
	 * is verified independently through
	 * {@link #verify(SemanticTool, StatementMessage)}.
	 */
	@Override
	public Map<StatementMessage, VerificationResult> verify(
			SemanticTool<A, D> tool,
			Collection<StatementMessage> warnings)
			throws SemanticException {
		init(tool);
		Map<StatementMessage, VerificationResult> results = new LinkedHashMap<>();
		for (StatementMessage warning : warnings)
			results.put(warning, verify(tool, warning));
		return results;
	}
}
