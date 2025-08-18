package it.unive.lisa.checks.semantic;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.checks.syntactic.CheckTool;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.util.file.FileManager;
import java.util.Collection;
import java.util.Map;

/**
 * An extension of {@link CheckTool} that also contains the results of the
 * fixpoint computation.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public class CheckToolWithAnalysisResults<A extends AbstractLattice<A>, D extends AbstractDomain<A>> extends CheckTool {

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
	public CheckToolWithAnalysisResults(
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
	public CheckToolWithAnalysisResults(
			CheckTool other,
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
		StatementStore<A> store = new StatementStore<>(result.getEntryState().bottom());
		for (Expression e : call.getParameters())
			store.put(e, result.getAnalysisStateAfter(e));

		try {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Analysis<A, D> analysis = new Analysis(getConfiguration().analysis);
			return callgraph.resolve(call, call.parameterTypes(store, analysis), null);
		} catch (CallResolutionException e) {
			return null;
		}
	}

}
