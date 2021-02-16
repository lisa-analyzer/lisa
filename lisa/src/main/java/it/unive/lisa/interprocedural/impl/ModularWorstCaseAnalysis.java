package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.analysis.*;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueIdentifier;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ModularWorstCaseAnalysis implements InterproceduralAnalysis {

    private static final Logger log = LogManager.getLogger(it.unive.lisa.interprocedural.impl.ModularWorstCaseAnalysis.class);


    /**
     * The cash of the fixpoints' results. {@link Map#keySet()} will contain all
     * the cfgs that have been added. If a key's values's
     * {@link Optional#isEmpty()} yields true, then the fixpoint for that key
     * has not be computed yet.
     */
    private final Map<CFG, Optional<CFGWithAnalysisResults<?, ?, ?>>> results;

    /**
     * The call graph used to resolve method calls
     */
    private CallGraph callgraph;

    private Program program;

    /**
     * Builds the call graph.
     */
    public ModularWorstCaseAnalysis() {
        this.results = new ConcurrentHashMap<>();
    }

    @Override
    public final void clear() {
        results.clear();
    }


    @Override
    public void build(Program program, CallGraph callgraph) throws InterproceduralAnalysisException {
        this.callgraph = callgraph;
        this.program = program;
    }

    @Override
    public final <A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>> void fixpoint(
            AnalysisState<A, H, V> entryState)
            throws FixpointException {
        for (CFG cfg : IterationLogger.iterate(log, program.getAllCFGs(), "Computing fixpoint over the whole program",
                "cfgs"))
            try {
                results.put(cfg, Optional.of(cfg.fixpoint(prepare(entryState, cfg), this)));
            } catch (SemanticException e) {
                throw new FixpointException("Error while creating the entrystate for " + cfg, e);
            }
    }

    private <A extends AbstractState<A, H, V>,
            H extends HeapDomain<H>,
            V extends ValueDomain<V>> AnalysisState<A, H, V> prepare(AnalysisState<A, H, V> entryState, CFG cfg)
            throws SemanticException {
        AnalysisState<A, H, V> prepared = entryState;
        for (Parameter arg : cfg.getDescriptor().getArgs())
            if (arg.getStaticType().isPointerType()) {
                prepared = prepared.smallStepSemantics(
                        new HeapReference(Caches.types().mkSingletonSet(arg.getStaticType()), arg.getName()),
                        cfg.getGenericProgramPoint());
                for (SymbolicExpression expr : prepared.getComputedExpressions())
                    prepared = prepared.assign((HeapIdentifier) expr,
                            new PushAny(Caches.types().mkSingletonSet(arg.getStaticType())),
                            cfg.getGenericProgramPoint());
            } else {
                ValueIdentifier id = new ValueIdentifier(Caches.types().mkSingletonSet(arg.getStaticType()),
                        arg.getName());
                prepared = prepared.assign(id, new PushAny(Caches.types().mkSingletonSet(arg.getStaticType())),
                        cfg.getGenericProgramPoint());
            }
        return prepared;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <A extends AbstractState<A, H, V>,
            H extends HeapDomain<H>,
            V extends ValueDomain<V>> CFGWithAnalysisResults<A, H, V> getAnalysisResultsOf(
            CFG cfg) {
        return (CFGWithAnalysisResults<A, H, V>) results.get(cfg).orElse(null);
    }

    @Override
    public final <A extends AbstractState<A, H, V>,
            H extends HeapDomain<H>,
            V extends ValueDomain<V>> AnalysisState<A, H, V> getAbstractResultOf(CFGCall call,
                                                                                 AnalysisState<A, H, V> entryState, Collection<SymbolicExpression>[] parameters)
            throws SemanticException {
        if (call.getStaticType().isVoidType())
            return entryState.top();

        return entryState.top().smallStepSemantics(new ValueIdentifier(call.getRuntimeTypes(), "ret_value"), call);
    }

    @Override
    public Call resolve(UnresolvedCall unresolvedCall) throws CallResolutionException {
        return callgraph.resolve(unresolvedCall);
    }

}
