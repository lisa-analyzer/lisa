package it.unive.lisa.interprocedural.inlining;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.CFGResults;
import it.unive.lisa.interprocedural.CallGraphBasedAnalysis;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.NoEntryPointException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.scoping.ScopingStrategy;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An inlining-based interprocedural analysis. This means that each call
 * receives its own result, with no "compacting" based on context or other
 * technique: each call receives its own result that is uniquely determined by
 * the call's entry strate. Recursions are not supported: either they converge
 * to a result, or the analysis (i) diverges if no maximum call stack depth is
 * set through the constructor, or (ii) terminates with an exception when the
 * maximum call stack depth has been reached.
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class InliningAnalysis<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		CallGraphBasedAnalysis<A, D> {

	private static final Logger LOG = LogManager.getLogger(InliningAnalysis.class);

	private int maxCallStackDepth;

	/**
	 * The current sensitivity token.
	 */
	private CallStackId<A> token;

	/**
	 * The results computed by this analysis.
	 */
	private FixpointResults<A> results;

	/**
	 * The kind of {@link WorkingSet} to use during this analysis.
	 */
	private WorkingSet<Statement> workingSet;

	/**
	 * The fixpoint configuration.
	 */
	private FixpointConfiguration conf;

	/**
	 * Builds the analysis, using an infinite call stack depth.
	 */
	public InliningAnalysis() {
		this(-1);
	}

	/**
	 * Builds the analysis.
	 *
	 * @param maxCallStackDepth the maximum call stack depth. A negative value
	 *                              means infinite depth. If a call chain
	 *                              exceeds this depth, an exception is raised
	 */
	public InliningAnalysis(
			int maxCallStackDepth) {
		this.maxCallStackDepth = maxCallStackDepth;
		this.token = CallStackId.create();
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		super.init(app, callgraph, policy, analysis);
		this.conf = null;
		this.results = null;
	}

	@Override
	public void fixpoint(
			AnalysisState<A> entryState,
			FixpointConfiguration conf)
			throws FixpointException {
		this.workingSet = conf.fixpointWorkingSet;
		this.conf = conf;

		// new fixpoint execution: reset
		CodeUnit unit = new CodeUnit(SyntheticLocation.INSTANCE, app.getPrograms()[0], "singleton");
		CFG singleton = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "singleton"));
		CallStackId<A> empty = token.startingId();
		AnalyzedCFG<A> graph = conf.optimize
				? new OptimizedAnalyzedCFG<>(singleton, empty, entryState.bottom(), this)
				: new AnalyzedCFG<>(singleton, empty, entryState);
		CFGResults<A> value = new CFGResults<>(graph);
		this.results = new FixpointResults<>(value.top());

		if (app.getEntryPoints().isEmpty())
			throw new NoEntryPointException();

		Collection<CFG> entryPoints = new TreeSet<>(
				(
						c1,
						c2) -> c1.getDescriptor().getLocation().compareTo(c2.getDescriptor().getLocation()));
		entryPoints.addAll(app.getEntryPoints());

		for (CFG cfg : IterationLogger.iterate(LOG, entryPoints, "Processing entrypoints", "entries"))
			try {
				token = empty;
				AnalysisState<A> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);
				results.putResult(
						cfg,
						empty,
						cfg.fixpoint(entryStateCFG, this, workingSet.mk(), conf, empty));
			} catch (SemanticException e) {
				throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
			} catch (FixpointException e) {
				throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
			}
	}

	@Override
	public Collection<AnalyzedCFG<A>> getAnalysisResultsOf(
			CFG cfg) {
		if (results.contains(cfg))
			return results.getState(cfg).getAll();
		else
			return Collections.emptySet();
	}

	/**
	 * Runs a fixpoint over the given {@link CFG}.
	 * 
	 * @param cfg        the target of the fixpoint
	 * @param token      the scope identifier that identifies the computation
	 * @param entryState the entry state for the fixpoint
	 * 
	 * @return the result of the fixpoint computation
	 * 
	 * @throws FixpointException if the fixpoint terminates abruptly
	 * @throws SemanticException if an exception happens while storing the
	 *                               result of the fixpoint
	 */
	private AnalyzedCFG<A> computeFixpoint(
			CFG cfg,
			CallStackId<A> token,
			AnalysisState<A> entryState)
			throws FixpointException,
			SemanticException {
		AnalyzedCFG<A> fixpointResult = cfg.fixpoint(entryState, this, workingSet.mk(), conf, token);
		Pair<Boolean, AnalyzedCFG<A>> res = results.putResult(cfg, token, fixpointResult);
		if (res.getLeft())
			throw new FixpointException("Inconsistent fixpoint result for " + cfg + " under token " + token);
		fixpointResult = res.getRight();
		return fixpointResult;
	}

	@Override
	public FixpointResults<A> getFixpointResults() {
		return results;
	}

	private Pair<AnalysisState<A>, ExpressionSet[]> prepareEntryState(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions,
			ScopeToken scope,
			CFG cfg)
			throws SemanticException {
		Parameter[] formals = cfg.getDescriptor().getFormals();

		// prepare the state for the call: hide the visible variables
		Pair<AnalysisState<A>,
				ExpressionSet[]> scoped = call.getProgram()
						.getFeatures()
						.getScopingStrategy()
						.scope(call, scope, entryState, analysis, parameters);
		AnalysisState<A> callState = scoped.getLeft();
		ExpressionSet[] locals = scoped.getRight();

		// assign parameters between the caller and the callee contexts
		ParameterAssigningStrategy strategy = call.getProgram().getFeatures().getAssigningStrategy();
		Pair<AnalysisState<A>,
				ExpressionSet[]> prepared = strategy.prepare(call, callState, this, expressions, formals, locals);
		return prepared;
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		callgraph.registerCall(call);

		if (maxCallStackDepth == 0)
			throw new SemanticException("Maximum call stack depth reached");

		CallStackId<A> callerToken = token;
		token = token.push(call, entryState);
		maxCallStackDepth--;
		ScopeToken scope = new ScopeToken(call);

		// we exclude erroneous/halting executions from the
		// initial states, since they will not be affected
		// by the call; they are still part of the result
		// and they will be lubbed with the return values
		AnalysisState<A> result = entryState.bottomExecution();
		AnalysisState<A> initialState = entryState.removeAllErrors(true);

		// compute the result over all possible targets, and take the lub of
		// the results
		for (CFG cfg : call.getTargetedCFGs()) {
			CFGResults<A> localResults = results.get(cfg);
			AnalyzedCFG<A> states = localResults == null ? null : localResults.get(token);
			Pair<AnalysisState<A>,
					ExpressionSet[]> prepared = prepareEntryState(
							call,
							initialState,
							parameters,
							expressions,
							scope,
							cfg);

			AnalysisState<A> exitState;
			if (states != null)
				// no need to compute the fixpoint: we already have an
				// exact approximation of the result having the same
				// call stack and entry states
				exitState = states.getExitState();
			else {
				// compute the result with a fixpoint iteration
				AnalyzedCFG<A> fixpointResult = null;
				try {
					fixpointResult = computeFixpoint(cfg, token, prepared.getLeft());
				} catch (FixpointException e) {
					throw new SemanticException("Exception during the interprocedural analysis", e);
				}

				exitState = initialState.bottom();
				for (Statement exit : fixpointResult.getAllExitpoints())
					exitState = exitState.lub(
							analysis.removeCaughtErrors(
									fixpointResult.getAnalysisStateAfter(exit),
									exit));
			}

			// save the resulting state
			ScopingStrategy strategy = call.getProgram().getFeatures().getScopingStrategy();
			AnalysisState<A> callres = strategy.unscope(call, scope, exitState, analysis);
			callres = analysis.transferThrowers(callres, call, cfg);
			callres = analysis.onCallReturn(entryState, callres, call);
			result = result.lub(callres);
		}

		token = callerToken;
		maxCallStackDepth++;
		return result;
	}

}
