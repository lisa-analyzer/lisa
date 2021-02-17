package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.*;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<CFG, Map<ContextSensitiveToken, CFGWithAnalysisResults<A, H, V>>> results;

    private final ContextSensitiveToken token;

    private class CFGResults {
        private Map<ContextSensitiveToken, Pair<A, CFGWithAnalysisResults<A, H, V>>> result = new ConcurrentHashMap<>();

        public Pair<A, CFGWithAnalysisResults<A, H, V>> getResult(ContextSensitiveToken token) {
            return result.get(token);
        }

        public void putResult(ContextSensitiveToken token, A entryState, CFGWithAnalysisResults<A, H, V> CFGresult)
            throws InterproceduralAnalysisException, SemanticException {
            Pair<A, CFGWithAnalysisResults<A, H, V>> previousResult = result.get(token);
            if(previousResult == null)
                result.put(token, Pair.of(entryState, CFGresult));
            else {
                AbstractState<A, H, V> previousEntryState = previousResult.getLeft();
                if(! previousEntryState.lessOrEqual(entryState))
                    throw new InterproceduralAnalysisException("Cannot reduce the entry state in the interprocedural analysis");
            }
        }
    }

    /**
     * Builds the call graph.
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
        for (CFG cfg : IterationLogger.iterate(log, program.getAllCFGs(), "Computing fixpoint over the whole program",
                "cfgs"))
            try {
                ConcurrentHashMap<ContextSensitiveToken, CFGWithAnalysisResults<A, H, V>>
                        value = new ConcurrentHashMap<>();
                value.put(token.empty(), cfg.fixpoint(prepareEntryStateOfEntryPoint(entryState, cfg), this));
                results.put(cfg, value);
            } catch (SemanticException e) {
                throw new FixpointException("Error while creating the entrystate for " + cfg, e);
            }
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Collection<CFGWithAnalysisResults<A, H, V>> getAnalysisResultsOf(
            CFG cfg) {
        return results.get(cfg).values();
    }

    @Override
    public final AnalysisState<A, H, V> getAbstractResultOf(CFGCall call, AnalysisState<A, H, V> entryState, Collection<SymbolicExpression>[] parameters)
            throws SemanticException {
        ContextSensitiveToken newToken = token.pushCall(call);

        //FIXME: go ahead here!

        if (call.getStaticType().isVoidType())
            return entryState.top();

        return entryState.top().smallStepSemantics(new ValueIdentifier(call.getRuntimeTypes(), "ret_value"), call);
    }


}
