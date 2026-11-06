package it.unive.lisa.checks.verification;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link WarningVerifier} that verifies each warning independently of the
 * others, by verifying the violation it reports in the results of the analysis
 * of the statement it is reported on. Implementations only have to define how a
 * single violation is verified in a single result
 * ({@link #verify(AnalyzedCFG, Statement, Violation)}).<br>
 * <br>
 * An {@link ErrorViolation} reported on a statement happens if a matching error
 * continuation is in the post-state of the statement, either because the
 * statement raises it or because it is propagated, uncaught, from the statement
 * raising it (e.g., an exception reaching the end of a method). Since errors
 * leaving a called method are attributed to the call, each error continuation
 * records a statement of the same cfg, its thrower: this class verifies error
 * violations by verifying, for each matching error continuation, that it is
 * raised by its thrower. Implementations thus only have to verify error
 * violations raised by the given statement.<br>
 * <br>
 * Failures while verifying a warning do not abort the verification: the warning
 * is deemed possible.
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

	private static final Logger LOG = LogManager.getLogger(IndividualWarningVerifier.class);

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
	 * support. {@link ErrorViolation}s given to this method are raised by the
	 * given statement (see the documentation of this class).
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
			return new VerificationResult(VerificationOutcome.POSSIBLE, "the warning reports no violation to verify");
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
			return new VerificationResult(VerificationOutcome.POSSIBLE,
					"no analysis result for the context of the warning");

		// an analysis without events, as the queries are not part of the
		// fixpoint computation
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Analysis<A, D> analysis = new Analysis(
				tool.getConfiguration().analysis,
				tool.getConfiguration().shouldSmashError);
		VerificationResult outcome = VerificationResult.REFUTED;
		try {
			for (AnalyzedCFG<A> result : results) {
				outcome = outcome.lub(verifyIn(analysis, result, st, violation));
				if (outcome.getOutcome() == VerificationOutcome.CONFIRMED)
					break;
			}
		} catch (SemanticException | RuntimeException | AssertionError e) {
			// assertions of the semantics of the statements might fail on the
			// states used by the verifier (e.g., when replaying statements)
			LOG.warn("Unable to verify " + violation + " at " + st.getLocation() + ", deeming it possible", e);
			return new VerificationResult(VerificationOutcome.POSSIBLE, "the verification failed: " + e.getMessage());
		}
		return outcome;
	}

	/**
	 * Verifies, directly at the given statement, an error violation whose error
	 * continuations might have been propagated from other statements. This is
	 * queried before verifying the continuations where they are raised, and it
	 * lets implementations exploit information that is only available where the
	 * warning is reported (e.g., that all executions reaching the end of a cfg
	 * do so with matching errors). If the result is
	 * {@link VerificationOutcome#POSSIBLE}, the continuations are verified
	 * where they are raised. The default implementation yields
	 * {@link VerificationResult#POSSIBLE}.
	 *
	 * @param result    the result containing the statement
	 * @param st        the statement on which the warning is issued
	 * @param violation the violation to verify
	 *
	 * @return the result of the verification
	 *
	 * @throws SemanticException if something goes wrong during the verification
	 */
	protected VerificationResult verifyPropagated(
			AnalyzedCFG<A> result,
			Statement st,
			ErrorViolation violation)
			throws SemanticException {
		return VerificationResult.POSSIBLE;
	}

	/**
	 * Verifies the given violation at the given statement, in the given result.
	 * Error violations are verified at the statements throwing the matching
	 * error continuations in the post-state of the statement (see the
	 * documentation of this class), excluding the ones that are caught by the
	 * error edges leaving the statement.
	 */
	private VerificationResult verifyIn(
			Analysis<A, D> analysis,
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation)
			throws SemanticException {
		if (!(violation instanceof ErrorViolation))
			return verify(result, st, violation);

		ErrorViolation error = (ErrorViolation) violation;
		VerificationResult direct = verifyPropagated(result, st, error);
		if (direct.getOutcome() != VerificationOutcome.POSSIBLE)
			return direct;

		// errors caught when leaving the statement do not propagate further
		AnalysisState<A> post = analysis.removeCaughtErrors(result.getAnalysisStateAfter(st), st);
		GenericMapLattice<AnalysisState.Error, ?> errors = post.getErrors();
		GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = post.getSmashedErrors();
		if (errors.isTop() || smashed.isTop())
			// we do not know which errors reach the statement
			return VerificationResult.POSSIBLE;

		Set<Statement> throwers = new LinkedHashSet<>();
		if (!errors.isBottom())
			for (AnalysisState.Error err : errors.getKeys())
				if (error.matches(err.getType()) && !post.getErrors().getState(err).getState().isBottom())
					throwers.add(err.getThrower());
		if (!smashed.isBottom())
			for (Map.Entry<Type, GenericSetLattice<Statement>> entry : smashed)
				if (error.matches(entry.getKey())) {
					if (entry.getValue().isTop())
						return VerificationResult.POSSIBLE;
					for (Statement thrower : entry.getValue())
						throwers.add(thrower);
				}

		// no matching error reaches the statement
		VerificationResult outcome = VerificationResult.REFUTED;
		for (Statement thrower : throwers) {
			// a confirmation tells which statement raises the error
			outcome = outcome.lub(verify(result, thrower, violation).withThrower(thrower));
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
