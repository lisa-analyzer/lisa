package it.unive.lisa.checks.semantic;

import it.unive.lisa.ReportingTool;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.checks.verification.WarningVerifier;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.outputs.messages.CFGMessage;
import it.unive.lisa.outputs.messages.ExpressionMessage;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.util.file.FileManager;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An extension of {@link ReportingTool} that also contains the results of the
 * fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public class SemanticTool<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		ReportingTool {

	private final Map<CFG, Collection<AnalyzedCFG<A>>> results;

	private final CallGraph callgraph;

	private final Analysis<A, D> analysis;

	/**
	 * Builds the tool, storing the given results.
	 * 
	 * @param configuration the configuration of the analysis
	 * @param fileManager   the file manager of the analysis
	 * @param results       the results to store
	 * @param callgraph     the callgraph that has been built during the
	 *                          analysis
	 * @param analysis      the analysis that has been executed to produce the
	 *                          results
	 */
	public SemanticTool(
			LiSAConfiguration configuration,
			FileManager fileManager,
			Map<CFG, Collection<AnalyzedCFG<A>>> results,
			CallGraph callgraph,
			Analysis<A, D> analysis) {
		super(configuration, fileManager);
		this.results = results;
		this.callgraph = callgraph;
		this.analysis = analysis;
	}

	/**
	 * Builds the tool, copying the given tool and storing the given results.
	 * 
	 * @param other     the tool to copy
	 * @param results   the results to store
	 * @param callgraph the callgraph that has been built during the analysis
	 * @param analysis  the analysis that has been executed to produce the
	 *                      results
	 */
	public SemanticTool(
			ReportingTool other,
			Map<CFG, Collection<AnalyzedCFG<A>>> results,
			CallGraph callgraph,
			Analysis<A, D> analysis) {
		super(other);
		this.results = results;
		this.callgraph = callgraph;
		this.analysis = analysis;
	}

	/**
	 * Yields the analysis results stored in this tool for the given
	 * {@link CFG}.
	 * 
	 * @param cfg the cfg whose results are to be retrieved
	 * 
	 * @return the results on the given cfg
	 */
	public Collection<AnalyzedCFG<A>> getResultOf(
			CFG cfg) {
		return results.get(cfg);
	}

	/**
	 * Yields all the analysis results stored in this tool, grouped by
	 * {@link CFG}.
	 * 
	 * @return an unmodifiable view of the results
	 */
	public Map<CFG, Collection<AnalyzedCFG<A>>> getResults() {
		return Collections.unmodifiableMap(results);
	}

	/**
	 * Yields the analysis that has been executed to produce the results stored
	 * in this tool.
	 * 
	 * @return the analysis
	 */
	public Analysis<A, D> getAnalysis() {
		return analysis;
	}

	/**
	 * Yields the {@link CallGraph} constructed during the analysis.
	 * 
	 * @return the callgraph
	 */
	public CallGraph getCallGraph() {
		return callgraph;
	}

	/**
	 * Yields all the {@link CodeMember}s that call the given one, according to
	 * the {@link CallGraph} that has been built during the analysis.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of callers code members
	 */
	public Collection<CodeMember> getCallers(
			CodeMember cm) {
		return callgraph.getCallers(cm);
	}

	/**
	 * Yields all the {@link CodeMember}s that are called by the given one,
	 * according to the {@link CallGraph} that has been built during the
	 * analysis.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of called code members
	 */
	public Collection<CodeMember> getCallees(
			CodeMember cm) {
		return callgraph.getCallees(cm);
	}

	/**
	 * Yields all the {@link Call}s that targets the given {@link CodeMember},
	 * according to the {@link CallGraph} that has been built during the
	 * analysis.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of calls that target the code member
	 */
	public Collection<Call> getCallSites(
			CodeMember cm) {
		return callgraph.getCallSites(cm);
	}

	/**
	 * Yields the resolved version of the given call, according to the
	 * {@link CallGraph} that has been built during the analysis. Yields
	 * {@code null} if the call cannot be resolved (i.e. an exception happened).
	 * 
	 * @param call   the call to resolve
	 * @param result the result that contains the post states of each parameter
	 * 
	 * @return the resolved version of the given call, or {@code null}
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public Call getResolvedVersion(
			UnresolvedCall call,
			AnalyzedCFG<A> result)
			throws SemanticException {
		StatementStore<A> store = new StatementStore<>(result.getEntryState()).bottom();
		for (Expression e : call.getParameters())
			store.put(e, result.getAnalysisStateAfter(e));

		try {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Analysis<A, D> analysis = new Analysis(
					getConfiguration().analysis,
					getConfiguration().shouldSmashError);
			return callgraph.resolve(call, call.parameterTypes(store, analysis, result.getId()), null);
		} catch (CallResolutionException e) {
			return null;
		}
	}

	/**
	 * Reports a new possible warning, with no reproducing inputs, with the
	 * given message on the given statement, in the given context (e.g., the one
	 * of the result that caused the warning).
	 * 
	 * @param statement the statement to warn on
	 * @param context   the context where the warning is reported
	 * @param message   the message of the warning
	 */
	public void warnOn(
			Statement statement,
			ScopeId<A> context,
			String message) {
		if (statement instanceof Expression)
			warnOn((Expression) statement, context, message);
		else
			addWarning(new StatementMessage(statement, message, context, false, null));
	}

	/**
	 * Reports a new possible warning with the given message on the given
	 * statement, reporting the given violation. After the semantic checks,
	 * LiSA verifies the violations of all warnings through the
	 * {@link WarningVerifier} selected in
	 * the configuration ({@link LiSAConfiguration#warningVerifier}), if any,
	 * possibly confirming or refuting the warning.
	 * 
	 * @param statement the statement to warn on
	 * @param violation the violation reported by the warning
	 * @param message   the message of the warning
	 */
	public void warnOn(
			Statement statement,
			Violation violation,
			String message) {
		warnOn(statement, null, violation, message);
	}

	/**
	 * Reports a new possible warning with the given message on the given
	 * statement, in the given context (e.g., the one of the result that caused
	 * the warning), reporting the given violation. After the semantic checks,
	 * LiSA verifies the violations of all warnings through the
	 * {@link WarningVerifier} selected in
	 * the configuration ({@link LiSAConfiguration#warningVerifier}), if any,
	 * possibly confirming or refuting the warning: the violation is verified
	 * only in the given context, or in all the contexts where the statement
	 * has been analyzed if the context is {@code null}.
	 * 
	 * @param statement the statement to warn on
	 * @param context   the context where the warning is reported (can be
	 *                      {@code null})
	 * @param violation the violation reported by the warning
	 * @param message   the message of the warning
	 */
	public void warnOn(
			Statement statement,
			ScopeId<A> context,
			Violation violation,
			String message) {
		if (statement instanceof Expression)
			addWarning(new ExpressionMessage((Expression) statement, message, context, violation));
		else
			addWarning(new StatementMessage(statement, message, context, violation));
	}

	/**
	 * Reports a new possible warning, with no reproducing inputs, with the
	 * given message on the given expression, in the given context (e.g., the
	 * one of the result that caused the warning).
	 * 
	 * @param expression the expression to warn on
	 * @param context    the context where the warning is reported
	 * @param message    the message of the warning
	 */
	public void warnOn(
			Expression expression,
			ScopeId<A> context,
			String message) {
		addWarning(new ExpressionMessage(expression, message, context, false, null));
	}

	/**
	 * Reports a new possible warning, with no reproducing inputs, with the
	 * given message on the given cfg, in the given context (e.g., the one of
	 * the result that caused the warning).
	 * 
	 * @param cfg     the cfg to warn on
	 * @param context the context where the warning is reported
	 * @param message the message of the warning
	 */
	public void warnOn(
			CFG cfg,
			ScopeId<A> context,
			String message) {
		addWarning(new CFGMessage(cfg, message, context, false, null));
	}

	/**
	 * Reports a new possible notice, with no reproducing inputs, with the given
	 * message on the given statement, in the given context (e.g., the one of
	 * the result that caused the notice).
	 * 
	 * @param statement the statement to notice on
	 * @param context   the context where the notice is reported
	 * @param message   the message of the notice
	 */
	public void noticeOn(
			Statement statement,
			ScopeId<A> context,
			String message) {
		noticeOn(statement, context, message, false, null);
	}

	/**
	 * Reports a new notice with the given message on the given statement, in
	 * the given context (e.g., the one of the result that caused the notice).
	 * If {@code statement} is an instance of {@link Expression}, then
	 * {@link #noticeOn(Expression, ScopeId, String, boolean, List)} is invoked.
	 * 
	 * @param statement         the statement to notice on
	 * @param context           the context where the notice is reported
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			Statement statement,
			ScopeId<A> context,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		if (statement instanceof Expression)
			noticeOn((Expression) statement, context, message, definite, reproducingInputs);
		else
			addNotice(new StatementMessage(statement, message, context, definite, reproducingInputs));
	}

	/**
	 * Reports a new possible notice, with no reproducing inputs, with the given
	 * message on the given expression, in the given context (e.g., the one of
	 * the result that caused the notice).
	 * 
	 * @param expression the expression to notice on
	 * @param context    the context where the notice is reported
	 * @param message    the message of the notice
	 */
	public void noticeOn(
			Expression expression,
			ScopeId<A> context,
			String message) {
		noticeOn(expression, context, message, false, null);
	}

	/**
	 * Reports a new notice with the given message on the given expression, in
	 * the given context (e.g., the one of the result that caused the notice).
	 * 
	 * @param expression        the expression to notice on
	 * @param context           the context where the notice is reported
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			Expression expression,
			ScopeId<A> context,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		addNotice(new ExpressionMessage(expression, message, context, definite, reproducingInputs));
	}

	/**
	 * Reports a new possible notice, with no reproducing inputs, with the given
	 * message on the given cfg, in the given context (e.g., the one of the
	 * result that caused the notice).
	 * 
	 * @param cfg     the cfg to notice on
	 * @param context the context where the notice is reported
	 * @param message the message of the notice
	 */
	public void noticeOn(
			CFG cfg,
			ScopeId<A> context,
			String message) {
		noticeOn(cfg, context, message, false, null);
	}

	/**
	 * Reports a new notice with the given message on the given cfg, in the
	 * given context (e.g., the one of the result that caused the notice).
	 * 
	 * @param cfg               the cfg to notice on
	 * @param context           the context where the notice is reported
	 * @param message           the message of the notice
	 * @param definite          whether or not the notice is definite, that is,
	 *                              if the reported behavior happens in at least
	 *                              one execution of the program
	 * @param reproducingInputs the inputs reproducing the behavior reported by
	 *                              the notice (can be {@code null} if no inputs
	 *                              are known)
	 */
	public void noticeOn(
			CFG cfg,
			ScopeId<A> context,
			String message,
			boolean definite,
			List<ReproducingInput> reproducingInputs) {
		addNotice(new CFGMessage(cfg, message, context, definite, reproducingInputs));
	}

}
