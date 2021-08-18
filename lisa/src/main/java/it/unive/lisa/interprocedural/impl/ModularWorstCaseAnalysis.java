package it.unive.lisa.interprocedural.impl;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
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
import it.unive.lisa.program.cfg.statement.OpenCall;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A worst case modular analysis were all method calls return top.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 */
public class ModularWorstCaseAnalysis<A extends AbstractState<A, H, V>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>> implements InterproceduralAnalysis<A, H, V> {

	private static final Logger LOG = LogManager.getLogger(ModularWorstCaseAnalysis.class);

	/**
	 * The program.
	 */
	private Program program;

	/**
	 * The cash of the fixpoints' results. {@link Map#keySet()} will contain all
	 * the cfgs that have been added. If a key's values's
	 * {@link Optional#isEmpty()} yields true, then the fixpoint for that key
	 * has not be computed yet.
	 */
	private final Map<CFG, Optional<CFGWithAnalysisResults<A, H, V>>> results;

	/**
	 * Builds the call graph.
	 */
	public ModularWorstCaseAnalysis() {
		this.results = new ConcurrentHashMap<>();
	}

	@Override
	public final void fixpoint(AnalysisState<A, H, V> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			int wideningThreshold) throws FixpointException {
		for (CFG cfg : IterationLogger.iterate(LOG, program.getAllCFGs(), "Computing fixpoint over the whole program",
				"cfgs"))
			try {
				AnalysisState<A, H, V> prepared = entryState;

				for (Parameter arg : cfg.getDescriptor().getArgs()) {
					ExternalSet<Type> all = Caches.types().mkSet(arg.getStaticType().allInstances());
					Variable id = new Variable(all, arg.getName(), arg.getAnnotations(), arg.getLocation());
					prepared = prepared.assign(id, new PushAny(all, arg.getLocation()), cfg.getGenericProgramPoint());
				}

				results.put(cfg, Optional
						.of(cfg.fixpoint(prepared, this, WorkingSet.of(fixpointWorkingSet), wideningThreshold)));
			} catch (SemanticException | AnalysisSetupException e) {
				throw new FixpointException("Error while creating the entrystate for " + cfg, e);
			}
	}

	@Override
	public final Collection<CFGWithAnalysisResults<A, H, V>> getAnalysisResultsOf(CFG cfg) {
		return Collections.singleton(results.get(cfg).orElse(null));
	}

	@Override
	public final AnalysisState<A, H, V> getAbstractResultOf(CFGCall call, AnalysisState<A, H, V> entryState,
			ExpressionSet<SymbolicExpression>[] parameters)
			throws SemanticException {
		if (call.getStaticType().isVoidType())
			return entryState.top();

		return entryState.top()
				.smallStepSemantics(new Variable(call.getRuntimeTypes(), "ret_value", call.getLocation()), call);
	}

	@Override
	public void init(Program program, CallGraph callgraph) throws InterproceduralAnalysisException {
		this.program = program;
	}

	@Override
	public Call resolve(UnresolvedCall unresolvedCall) throws CallResolutionException {
		OpenCall open = new OpenCall(unresolvedCall.getCFG(), unresolvedCall.getLocation(),
				unresolvedCall.getTargetName(), unresolvedCall.getStaticType(), unresolvedCall.getParameters());
		open.setRuntimeTypes(unresolvedCall.getRuntimeTypes());
		return open;
	}
}
