package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context sensitive interprocedural analysis. The context sensitivity is
 * tuned by the kind of {@link ContextSensitiveToken} used.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 */
public class ContextBasedAnalysis<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> extends CallGraphBasedAnalysis<A, H, V> {

	private static final Logger log = LogManager.getLogger(ContextBasedAnalysis.class);

	/**
	 * The cache of the fixpoints' results. {@link Map#keySet()} will contain
	 * all the cfgs that have been added. If a key's values's
	 * {@link Optional#isEmpty()} yields true, then the fixpoint for that key
	 * has not be computed yet.
	 */
	private FixpointResults<A, H, V> results;

	private final ContextSensitiveToken token;

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
	public ContextBasedAnalysis(ContextSensitiveToken token) {
		this.token = token.empty();
	}

	@Override
	public final void fixpoint(
			AnalysisState<A, H, V> entryState)
			throws FixpointException {
		if (program.getEntryPoints().isEmpty())
			throw new FixpointException("The program contains no entrypoints");

		TimerLogger.execAction(log, "Computing fixpoint over the whole program", () -> this.fixpointAux(entryState));
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

	private void fixpointAux(AnalysisState<A, H, V> entryState) throws AnalysisExecutionException {
		this.results = null;
		int iter = 0;
		do {
			log.info("Performing " + ordinal(iter + 1) + " fixpoint iteration");
			if (results != null)
				results.newIteration();
			for (CFG cfg : IterationLogger.iterate(log, program.getEntryPoints(), "Processing entrypoints", "entries"))
				try {
					CFGResults<A, H, V> value = new CFGResults<>(new CFGWithAnalysisResults<>(cfg, entryState));
					AnalysisState<A, H, V> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);
					if (results == null)
						this.results = new FixpointResults<>(value.top());
					results.putResult(cfg, token.empty(), cfg.fixpoint(entryStateCFG, this));
				} catch (SemanticException e) {
					throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
				} catch (FixpointException e) {
					throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
				}
			iter++;
		} while (results.lubbedSomething());
	}

	@Override
	public final Collection<CFGWithAnalysisResults<A, H, V>> getAnalysisResultsOf(CFG cfg) {
		if (results.contains(cfg))
			return results.getState(cfg).getAll();
		else
			return Collections.emptySet();
	}

	private Pair<AnalysisState<A, H, V>, AnalysisState<A, H, V>> getEntryAndExit(CFG cfg, ContextSensitiveToken token)
			throws SemanticException {
		if (!results.contains(cfg))
			return null;
		CFGResults<A, H, V> cfgresult = results.getState(cfg);
		if (!cfgresult.contains(token))
			return null;
		CFGWithAnalysisResults<A, H, V> analysisresult = cfgresult.getState(token);
		return Pair.of(analysisresult.getEntryState(), analysisresult.getExitState());
	}

	@Override
	public final AnalysisState<A, H, V> getAbstractResultOf(CFGCall call, AnalysisState<A, H, V> entryState,
			ExpressionSet<SymbolicExpression>[] parameters)
			throws SemanticException {
		ScopeToken scope = new ScopeToken(call);
		ContextSensitiveToken newToken = token.pushToken(scope);
		AnalysisState<A, H, V> result = entryState.bottom();

		for (CFG cfg : call.getTargets()) {
			Pair<AnalysisState<A, H, V>, AnalysisState<A, H, V>> states = getEntryAndExit(cfg, newToken);

			// prepare the state for the call: hide the visible variables
			AnalysisState<A, H, V> callState = entryState.pushScope(scope);
			AnalysisState<A, H, V> exitState;

			if (states != null && entryState.lessOrEqual(states.getLeft()))
				// no need to compute the fixpoint: we already have an
				// approximation
				exitState = states.getRight();
			else {
				// prepare the state for the call: assign the value to each
				// parameter
				AnalysisState<A, H, V> prepared = callState;
				for (int i = 0; i < parameters.length; i++) {
					AnalysisState<A, H, V> temp = prepared.bottom();
					Parameter parameter = cfg.getDescriptor().getArgs()[i];
					Identifier parid = new Variable(
							Caches.types().mkSet(parameter.getStaticType().allInstances()),
							parameter.getName(), parameter.getAnnotations());
					for (SymbolicExpression exp : parameters[i])
						temp = temp.lub(prepared.assign(parid, exp.pushScope(scope), cfg.getGenericProgramPoint()));
					prepared = temp;
				}

				// compute the result
				CFGWithAnalysisResults<A, H, V> fixpointResult = null;
				try {
					fixpointResult = computeFixpoint(newToken, cfg, prepared);
				} catch (FixpointException | InterproceduralAnalysisException e) {
					throw new SemanticException("Exception during the interprocedural analysis", e);
				}

				// store returned variables into the meta variable
				exitState = fixpointResult.getExitState();
			}

			// store the return value of the call inside the meta variable
			AnalysisState<A, H, V> tmp = callState.bottom();
			Identifier meta = (Identifier) call.getMetaVariable().pushScope(scope);
			for (SymbolicExpression ret : exitState.getComputedExpressions())
				tmp = tmp.lub(exitState.assign(meta, ret, call));

			// save the resulting state
			result = result.lub(tmp.popScope(scope));
		}

		return result;
	}

	private CFGWithAnalysisResults<A, H, V> computeFixpoint(ContextSensitiveToken newToken, CFG cfg,
			AnalysisState<A, H, V> computedEntryState)
			throws FixpointException, InterproceduralAnalysisException, SemanticException {
		CFGWithAnalysisResults<A, H, V> fixpointResult = cfg.fixpoint(computedEntryState, this);
		fixpointResult.setId(newToken.toString());
		results.putResult(cfg, newToken, fixpointResult);
		return fixpointResult;
	}

}
