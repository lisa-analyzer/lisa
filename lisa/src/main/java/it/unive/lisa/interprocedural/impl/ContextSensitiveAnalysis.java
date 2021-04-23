package it.unive.lisa.interprocedural.impl;

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
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context sensitive inteprocedural analysis.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 */
public class ContextSensitiveAnalysis<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> extends CallGraphBasedAnalysis<A, H, V> {

	private static final Logger log = LogManager.getLogger(ContextSensitiveAnalysis.class);

	/**
	 * The cache of the fixpoints' results. {@link Map#keySet()} will contain
	 * all the cfgs that have been added. If a key's values's
	 * {@link Optional#isEmpty()} yields true, then the fixpoint for that key
	 * has not be computed yet.
	 */
	private final Map<CFG, CFGResults> results;

	private final ContextSensitiveToken token;

	private class CFGResults {
		private Map<ContextSensitiveToken, CFGWithAnalysisResults<A, H, V>> result = new ConcurrentHashMap<>();

		public CFGWithAnalysisResults<A, H, V> getResult(ContextSensitiveToken token) {
			return result.get(token);
		}

		public void putResult(ContextSensitiveToken token, CFGWithAnalysisResults<A, H, V> CFGresult)
				throws InterproceduralAnalysisException, SemanticException {
			CFGWithAnalysisResults<A, H, V> previousResult = result.get(token);
			if (previousResult == null)
				result.put(token, CFGresult);
			else {
				if (!previousResult.getEntryState().lessOrEqual(CFGresult.getEntryState()))
					throw new InterproceduralAnalysisException(
							"Cannot reduce the entry state in the interprocedural analysis");
			}
		}

		public Collection<CFGWithAnalysisResults<A, H, V>> getAll() {
			return this.result.values();
		}
	}

	/**
	 * Builds the call graph.
	 *
	 * @param token an instance of the tokens to be used to partition w.r.t.
	 *                  context sensitivity
	 */
	public ContextSensitiveAnalysis(ContextSensitiveToken token) {
		this.token = token.empty();
		this.results = new ConcurrentHashMap<>();
	}

	@Override
	public final void clear() {
		results.clear();
	}

	@Override
	public final void fixpoint(
			AnalysisState<A, H, V> entryState)
			throws FixpointException {
		for (CFG cfg : IterationLogger.iterate(log, program.getEntryPoints(),
				"Computing fixpoint over the whole program",
				"cfgs"))
			try {
				CFGResults value = new CFGResults();
				AnalysisState<A, H, V> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);
				value.putResult(token.empty(), cfg.fixpoint(entryStateCFG, this));
				results.put(cfg, value);
			} catch (SemanticException | InterproceduralAnalysisException e) {
				throw new FixpointException("Error while creating the entrystate for " + cfg, e);
			}
	}

	@Override
	public final Collection<CFGWithAnalysisResults<A, H, V>> getAnalysisResultsOf(CFG cfg) {
		if (results.get(cfg) != null)
			return results.get(cfg).getAll();
		else
			return Collections.emptySet();
	}

	private Pair<AnalysisState<A, H, V>, AnalysisState<A, H, V>> getEntryAndExit(CFG cfg, ContextSensitiveToken token)
			throws SemanticException {
		CFGResults cfgresult = results.get(cfg);
		CFGWithAnalysisResults<A, H, V> analysisresult = null;
		if (cfgresult != null)
			analysisresult = cfgresult.getResult(token);
		if (analysisresult != null)
			return Pair.of(analysisresult.getEntryState(), analysisresult.getExitState());
		return null;
	}

	@Override
	public final AnalysisState<A, H, V> getAbstractResultOf(CFGCall call, AnalysisState<A, H, V> entryState,
			ExpressionSet<SymbolicExpression>[] parameters)
			throws SemanticException {
		ContextSensitiveToken newToken = token.pushCall(call);
		AnalysisState<A, H, V> result = entryState.bottom();

		for (CFG cfg : call.getTargets()) {
			Pair<AnalysisState<A, H, V>, AnalysisState<A, H, V>> states = getEntryAndExit(cfg, newToken);
			if (states != null && entryState.lessOrEqual(states.getLeft())) {
				// no need to compute the fixpoint: we already have an
				// approximation
				result = result.lub(states.getRight());
				continue;
			}

			// prepare the state for the call: hide the visible variables, and
			// then assign
			// the value to each parameter
			ScopeToken scope = new ScopeToken(call);
			AnalysisState<A, H, V> callState = entryState.pushScope(scope);
			for (int i = 0; i < parameters.length; i++) {
				AnalysisState<A, H, V> temp = callState.bottom();
				Parameter parameter = cfg.getDescriptor().getArgs()[i];
				Identifier parid = new Variable(
						Caches.types().mkSet(parameter.getStaticType().allInstances()),
						parameter.getName());
				for (SymbolicExpression exp : parameters[i])
					temp = temp.lub(callState.assign(parid, exp.pushScope(scope), cfg.getGenericProgramPoint()));
				callState = temp;
			}

			// compute the result
			CFGWithAnalysisResults<A, H, V> fixpointResult = null;
			try {
				fixpointResult = computeFixpoint(newToken, cfg, callState);
			} catch (FixpointException | InterproceduralAnalysisException e) {
				throw new SemanticException("Exception during the interprocedural analysis", e);
			}

			// store returned variables into the meta variable
			AnalysisState<A, H, V> exitState = fixpointResult.getExitState();
			AnalysisState<A, H, V> tmp = entryState.bottom();
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
		CFGResults result = this.results.get(cfg);
		if (result == null)
			result = new CFGResults();
		result.putResult(newToken, fixpointResult);
		return fixpointResult;
	}

}
