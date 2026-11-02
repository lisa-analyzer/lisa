package it.unive.lisa.triage.reachability;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.ConditionViolation;
import it.unive.lisa.checks.verification.ErrorViolation;
import it.unive.lisa.checks.verification.ReachViolation;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.checks.verification.IndividualWarningVerifier;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Type;

/**
 * An {@link IndividualWarningVerifier} that relies on the {@link Reachability} of
 * statements, together with the satisfiability of conditions and the presence
 * of error continuations in the analysis results, to verify violations:
 * <ul>
 * <li>a {@link ReachViolation} is refuted if the statement is
 * {@link ReachabilityStatus#UNREACHABLE}, confirmed if it is
 * {@link ReachabilityStatus#REACHABLE}, and possible otherwise;</li>
 * <li>a {@link ConditionViolation} is refuted if the statement where the
 * condition is evaluated is unreachable, or if the condition does not hold in
 * its post-state; it is confirmed if that statement is reachable and the
 * condition always holds in its post-state; it is possible otherwise;</li>
 * <li>an {@link ErrorViolation} raised by the statement (propagated errors are
 * redirected to the statements raising them, see
 * {@link IndividualWarningVerifier}) is refuted if the statement is unreachable or
 * if its post-state does not contain matching error continuations; it is
 * confirmed if the statement is reachable and its post-state contains only
 * matching error continuations; it is possible otherwise.</li>
 * </ul>
 * Note that {@link ReachabilityStatus#REACHABLE} assumes that all loops
 * terminate: confirmations are only as strong as this assumption. Moreover,
 * confirmed results do not carry the concrete values causing the violation.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public class ReachabilityVerifier<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		IndividualWarningVerifier<A, D> {

	private Reachability<A, D> reachability;

	private Analysis<A, D> analysis;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void init(
			SemanticTool<A, D> tool)
			throws SemanticException {
		// we use an analysis without events, as the queries are not
		// part of the fixpoint computation
		analysis = new Analysis(
				tool.getConfiguration().analysis,
				tool.getConfiguration().shouldSmashError);
		reachability = new Reachability<>(tool.getResults(), tool.getCallGraph(), analysis);
	}

	/**
	 * Yields the {@link Reachability} algorithm used by this verifier, built on
	 * the results of the tool passed to {@link #init(SemanticTool)}.
	 *
	 * @return the reachability algorithm, or {@code null} if this verifier has
	 *             not been initialized yet
	 */
	public Reachability<A, D> getReachability() {
		return reachability;
	}

	@Override
	public VerificationResult verify(
			AnalyzedCFG<A> result,
			Statement st,
			Violation violation)
			throws SemanticException {
		if (violation instanceof ReachViolation)
			return verifyReach(result, st);
		if (violation instanceof ConditionViolation)
			return verifyCondition(result, (ConditionViolation) violation);
		if (violation instanceof ErrorViolation)
			return verifyError(result, st, (ErrorViolation) violation);
		return VerificationResult.POSSIBLE;
	}

	private VerificationResult verifyReach(
			AnalyzedCFG<A> result,
			Statement st)
			throws SemanticException {
		ReachabilityStatus status = reachability.getReachability(result, st);
		if (status == ReachabilityStatus.UNREACHABLE)
			return VerificationResult.REFUTED;
		if (status == ReachabilityStatus.REACHABLE)
			return VerificationResult.CONFIRMED;
		return VerificationResult.POSSIBLE;
	}

	private VerificationResult verifyCondition(
			AnalyzedCFG<A> result,
			ConditionViolation violation)
			throws SemanticException {
		Statement where = violation.getWhere();
		ReachabilityStatus status = reachability.getReachability(result, where);
		if (status == ReachabilityStatus.UNREACHABLE)
			return VerificationResult.REFUTED;

		AnalysisState<A> post = result.getAnalysisStateAfter(where);
		if (post.getExecution().isBottom())
			return VerificationResult.REFUTED;

		Satisfiability sat = analysis.satisfies(post, violation.getCondition(), result.getId(), where);
		if (sat == Satisfiability.NOT_SATISFIED || sat == Satisfiability.BOTTOM)
			return VerificationResult.REFUTED;
		if (sat == Satisfiability.SATISFIED && status == ReachabilityStatus.REACHABLE)
			return VerificationResult.CONFIRMED;
		return VerificationResult.POSSIBLE;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * If the statement is an exit point of its cfg, the entry of the cfg is
	 * {@link ReachabilityStatus#REACHABLE}, and all the exit points of the cfg
	 * are only reached with matching errors (that is, their normal and halting
	 * states are bottom, and they contain no other error), then every
	 * terminating execution of the cfg ends with a matching error, and the
	 * violation is confirmed. This holds even if no single statement always
	 * raises the error (e.g., if two branches raise it).
	 */
	@Override
	protected VerificationResult verifyPropagated(
			AnalyzedCFG<A> result,
			Statement st,
			ErrorViolation violation)
			throws SemanticException {
		CFG cfg = st.getCFG();
		if (!cfg.getAllExitpoints().contains(st)
				|| reachability.getEntryReachability(result) != ReachabilityStatus.REACHABLE)
			return VerificationResult.POSSIBLE;

		boolean matching = false;
		for (Statement exit : cfg.getAllExitpoints()) {
			AnalysisState<A> post = result.getAnalysisStateAfter(exit);
			GenericMapLattice<Error, ProgramState<A>> errors = post.getErrors();
			GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = post.getSmashedErrors();
			if (errors.isTop() || smashed.isTop()
					|| !post.getExecution().getState().isBottom()
					|| !post.getHalt().getState().isBottom())
				return VerificationResult.POSSIBLE;
			if (!errors.isBottom())
				for (Error error : errors.getKeys())
					if (!errors.getState(error).getState().isBottom())
						if (violation.matches(error.getType()))
							matching = true;
						else
							return VerificationResult.POSSIBLE;
			if (!smashed.isBottom() && !post.getSmashedErrorsState().getState().isBottom())
				for (Type type : smashed.getKeys())
					if (!smashed.getState(type).isBottom())
						if (violation.matches(type))
							matching = true;
						else
							return VerificationResult.POSSIBLE;
		}
		return matching ? VerificationResult.CONFIRMED : VerificationResult.POSSIBLE;
	}

	private VerificationResult verifyError(
			AnalyzedCFG<A> result,
			Statement st,
			ErrorViolation violation)
			throws SemanticException {
		ReachabilityStatus status = reachability.getReachability(result, st);
		if (status == ReachabilityStatus.UNREACHABLE)
			return VerificationResult.REFUTED;

		// the post-states of sub-expressions might not contain the errors they
		// raise: the post-state of the node containing the statement is used
		Statement node = st instanceof Expression ? ((Expression) st).getRootStatement() : st;
		if (!st.getCFG().containsNode(node))
			node = st;
		AnalysisState<A> post = result.getAnalysisStateAfter(node);
		GenericMapLattice<Error, ProgramState<A>> errors = post.getErrors();
		GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = post.getSmashedErrors();
		if (errors.isTop() || smashed.isTop())
			// we cannot tell which errors are present
			return VerificationResult.POSSIBLE;

		boolean matching = false;
		// a state is unreachable if its abstract state is bottom
		boolean others = !post.getExecution().getState().isBottom() || !post.getHalt().getState().isBottom();
		if (!errors.isBottom())
			for (Error error : errors.getKeys())
				if (!errors.getState(error).getState().isBottom())
					if (violation.matches(error.getType()))
						matching = true;
					else
						others = true;

		if (!smashed.isBottom() && !post.getSmashedErrorsState().getState().isBottom())
			for (Type type : smashed.getKeys())
				if (!smashed.getState(type).isBottom())
					if (violation.matches(type))
						matching = true;
					else
						others = true;

		if (!matching)
			return VerificationResult.REFUTED;
		if (!others && status == ReachabilityStatus.REACHABLE)
			return VerificationResult.CONFIRMED;
		return VerificationResult.POSSIBLE;
	}
}
