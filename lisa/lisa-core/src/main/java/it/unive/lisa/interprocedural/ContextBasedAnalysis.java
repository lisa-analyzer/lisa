package it.unive.lisa.interprocedural;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.cfg.ImplementedCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context sensitive interprocedural analysis. The context sensitivity is
 * tuned by the kind of {@link ContextSensitivityToken} used.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 * @param <T> the type domain
 */
public class ContextBasedAnalysis<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>> extends CallGraphBasedAnalysis<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(ContextBasedAnalysis.class);

	/**
	 * The cache of the fixpoints' results. {@link Map#keySet()} will contain
	 * all the cfgs that have been added. If a key's values's
	 * {@link Optional#isEmpty()} yields true, then the fixpoint for that key
	 * has not be computed yet.
	 */
	private FixpointResults<A, H, V, T> results;

	private ContextSensitivityToken token;

	private final Collection<ImplementedCFG> fixpointTriggers;

	private Class<? extends WorkingSet<Statement>> fixpointWorkingSet;

	private int wideningThreshold;

	/**
	 * Builds the analysis, using {@link SingleScopeToken}s.
	 */
	public ContextBasedAnalysis() {
		this(SingleScopeToken.getSingleton());
	}

	/**
	 * Builds the analysis.
	 *
	 * @param token an instance of the tokens to be used to partition w.r.t.
	 *                  context sensitivity
	 */
	public ContextBasedAnalysis(ContextSensitivityToken token) {
		this.token = token.empty();
		fixpointTriggers = new HashSet<>();
	}

	@Override
	public void fixpoint(
			AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			int wideningThreshold)
			throws FixpointException {
		this.results = null;
		this.fixpointWorkingSet = fixpointWorkingSet;
		this.wideningThreshold = wideningThreshold;

		if (program.getEntryPoints().isEmpty())
			throw new NoEntryPointException();

		TimerLogger.execAction(LOG, "Computing fixpoint over the whole program",
				() -> this.fixpointAux(entryState, fixpointWorkingSet, wideningThreshold));
	}

	private static String ordinal(int i) {
		int n = i % 100;
		if (n == 11 || n == 12 || n == 13 || n % 10 == 0 || n % 10 > 3)
			return i + "th";

		if (n % 10 == 1)
			return i + "st";

		if (n % 10 == 2)
			return i + "nd";

		return i + "rd";
	}

	private void fixpointAux(AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			int wideningThreshold) throws AnalysisExecutionException {
		int iter = 0;
		do {
			LOG.info("Performing {} fixpoint iteration", ordinal(iter + 1));
			fixpointTriggers.clear();
			for (ImplementedCFG cfg : IterationLogger.iterate(LOG, program.getEntryPoints(), "Processing entrypoints",
					"entries"))
				try {
					CFGResults<A, H, V, T> value = new CFGResults<>(new CFGWithAnalysisResults<>(cfg, entryState));
					AnalysisState<A, H, V, T> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);
					if (results == null)
						this.results = new FixpointResults<>(value.top());
					results.putResult(cfg, token.empty(),
							cfg.fixpoint(entryStateCFG, this, WorkingSet.of(fixpointWorkingSet), wideningThreshold));
				} catch (SemanticException | AnalysisSetupException e) {
					throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
				} catch (FixpointException e) {
					throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
				}

			// starting from the callers of the cfgs that needed a lub,
			// find out the complete set of cfgs that might need to be
			// processed again
			VisitOnceWorkingSet<ImplementedCFG> ws = VisitOnceWorkingSet.mk(FIFOWorkingSet.mk());
			fixpointTriggers.forEach(cfg -> callgraph.getCallers(cfg).stream().filter(ImplementedCFG.class::isInstance)
					.map(ImplementedCFG.class::cast).forEach(ws::push));
			while (!ws.isEmpty())
				callgraph.getCallers(ws.pop()).stream().filter(ImplementedCFG.class::isInstance)
						.map(ImplementedCFG.class::cast)
						.forEach(ws::push);

			ws.getSeen().forEach(results::forget);

			iter++;
		} while (!fixpointTriggers.isEmpty());
	}

	@Override
	public Collection<CFGWithAnalysisResults<A, H, V, T>> getAnalysisResultsOf(ImplementedCFG cfg) {
		if (results.contains(cfg))
			return results.getState(cfg).getAll();
		else
			return Collections.emptySet();
	}

	private Pair<AnalysisState<A, H, V, T>, AnalysisState<A, H, V, T>> getEntryAndExit(ImplementedCFG cfg)
			throws SemanticException {
		if (!results.contains(cfg))
			return null;
		CFGResults<A, H, V, T> cfgresult = results.getState(cfg);
		if (!cfgresult.contains(token))
			return null;
		CFGWithAnalysisResults<A, H, V, T> analysisresult = cfgresult.getState(token);
		return Pair.of(analysisresult.getEntryState(), analysisresult.getExitState());
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		ScopeToken scope = new ScopeToken(call);
		token = token.pushToken(scope);
		AnalysisState<A, H, V, T> result = entryState.bottom();

		for (ImplementedCFG cfg : call.getTargets()) {
			Pair<AnalysisState<A, H, V, T>, AnalysisState<A, H, V, T>> states = getEntryAndExit(cfg);

			// prepare the state for the call: hide the visible variables
			AnalysisState<A, H, V, T> callState = entryState.pushScope(scope);

			Parameter[] formals = cfg.getDescriptor().getFormals();
			@SuppressWarnings("unchecked")
			ExpressionSet<SymbolicExpression>[] actuals = new ExpressionSet[parameters.length];

			for (int i = 0; i < parameters.length; i++)
				actuals[i] = parameters[i].pushScope(scope);

			Pair<AnalysisState<A, H, V, T>,
					ExpressionSet<SymbolicExpression>[]> prepared = call.getAssigningStrategy().prepare(call, callState,
							this, expressions, formals, actuals);

			AnalysisState<A, H, V, T> exitState;
			if (states != null && prepared.getLeft().lessOrEqual(states.getLeft()))
				// no need to compute the fixpoint: we already have an
				// approximation
				exitState = states.getRight();
			else {
				// compute the result
				CFGWithAnalysisResults<A, H, V, T> fixpointResult = null;
				try {
					fixpointResult = computeFixpoint(cfg, token, prepared.getLeft());
				} catch (FixpointException | AnalysisSetupException e) {
					throw new SemanticException("Exception during the interprocedural analysis", e);
				}

				exitState = fixpointResult.getExitState();
			}

			// store the return value of the call inside the meta variable
			AnalysisState<A, H, V, T> tmp = callState.bottom();
			Identifier meta = (Identifier) call.getMetaVariable().pushScope(scope);
			for (SymbolicExpression ret : exitState.getComputedExpressions())
				tmp = tmp.lub(exitState.assign(meta, ret, call));

			// save the resulting state
			result = result.lub(tmp.popScope(scope));
		}

		token = token.popToken();

		callgraph.registerCall(call);

		return result;
	}

	private CFGWithAnalysisResults<A, H, V, T> computeFixpoint(ImplementedCFG cfg, ContextSensitivityToken localToken,
			AnalysisState<A, H, V, T> computedEntryState)
			throws FixpointException, SemanticException, AnalysisSetupException {
		CFGWithAnalysisResults<A, H, V, T> fixpointResult = cfg.fixpoint(computedEntryState, this,
				WorkingSet.of(fixpointWorkingSet), wideningThreshold);
		fixpointResult.setId(localToken.toString());
		Pair<Boolean, CFGWithAnalysisResults<A, H, V, T>> res = results.putResult(cfg, localToken, fixpointResult);
		if (Boolean.TRUE.equals(res.getLeft()))
			fixpointTriggers.add(cfg);
		return res.getRight();
	}
}
