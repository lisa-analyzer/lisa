package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.*;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context sensitive inteprocedural analysis.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 */
public class ContextSensitiveInterproceduralAnalysis<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> extends CallGraphBasedInterproceduralAnalysis<A, H, V> {

	private static final Logger log = LogManager.getLogger(ContextSensitiveInterproceduralAnalysis.class);

	/**
	 * The cash of the fixpoints' results. {@link Map#keySet()} will contain all
	 * the cfgs that have been added. If a key's values's
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
	public ContextSensitiveInterproceduralAnalysis(ContextSensitiveToken token) {
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
	@SuppressWarnings("unchecked")
	public final Collection<CFGWithAnalysisResults<A, H, V>> getAnalysisResultsOf(
			CFG cfg) {
		if (results.get(cfg) != null)
			return results.get(cfg).getAll();
		else
			return Collections.emptySet();
	}

	@Override
	public final AnalysisState<A, H, V> getAbstractResultOf(CFGCall call, AnalysisState<A, H, V> entryState,
			Collection<SymbolicExpression>[] parameters)
			throws SemanticException {
		ContextSensitiveToken newToken = token.pushCall(call);
		AnalysisState<A, H, V> exitState = entryState.bottom();

		for (CFG cfg : call.getTargets()) {
			AnalysisState<A, H, V> computedEntryState = entryState;
			CFGResults cfgresult = results.get(cfg);
			CFGWithAnalysisResults<A, H, V> analysisresult = cfgresult != null ? cfgresult.getResult(newToken) : null;
			AnalysisState<A, H, V> entry = analysisresult != null ? analysisresult.getEntryState() : null;
			if (entry != null && entryState.lessOrEqual(entry))
				exitState = exitState.lub(analysisresult.getExitState());
			else
				try {
					int i = 0;
					for (Collection<SymbolicExpression> par : parameters) {
						AnalysisState<A, H, V> temp = computedEntryState.bottom();
						Parameter parameter = cfg.getDescriptor().getArgs()[i];
						Identifier parid = new ValueIdentifier(Caches.types().mkSingletonSet(parameter.getStaticType()),
								parameter.getName());
						for (SymbolicExpression exp : par)
							temp = temp.lub(computedEntryState.assign(parid, exp, cfg.getGenericProgramPoint()));
						computedEntryState = temp;
						i++;
					}
					CFGWithAnalysisResults<A, H, V> fixpointResult = computeFixpoint(newToken, cfg, computedEntryState);
					exitState = exitState.lub(fixpointResult.getExitState());
				} catch (FixpointException | InterproceduralAnalysisException e) {
					throw new SemanticException("Exception during the interprocedural analysis", e);
				}
		}
		return exitState;
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
