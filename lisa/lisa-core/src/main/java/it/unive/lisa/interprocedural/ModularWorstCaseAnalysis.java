package it.unive.lisa.interprocedural;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CFGWithAnalysisResults;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;

/**
 * A worst case modular analysis were all cfg calls are treated as open calls.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 */
public class ModularWorstCaseAnalysis<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>, T extends TypeDomain<T>> implements InterproceduralAnalysis<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(ModularWorstCaseAnalysis.class);

	/**
	 * The program.
	 */
	private Program program;

	/**
	 * The policy used for computing the result of cfg calls.
	 */
	private OpenCallPolicy policy;

	/**
	 * The cash of the fixpoints' results. {@link Map#keySet()} will contain all
	 * the cfgs that have been added. If a key's values's
	 * {@link Optional#isEmpty()} yields true, then the fixpoint for that key
	 * has not be computed yet.
	 */
	private final Map<CFG, Optional<CFGWithAnalysisResults<A, H, V, T>>> results;

	/**
	 * Builds the interprocedural analysis.
	 */
	public ModularWorstCaseAnalysis() {
		this.results = new ConcurrentHashMap<>();
	}

	@Override
	public void fixpoint(AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			int wideningThreshold) throws FixpointException {
		for (CFG cfg : IterationLogger.iterate(LOG, program.getAllCFGs(), "Computing fixpoint over the whole program",
				"cfgs"))
			try {
				AnalysisState<A, H, V, T> prepared = entryState;

				for (Parameter arg : cfg.getDescriptor().getFormals()) {
					Variable id = new Variable(arg.getStaticType(), arg.getName(), arg.getAnnotations(),
							arg.getLocation());
					prepared = prepared.assign(id, new PushAny(arg.getStaticType(), arg.getLocation()),
							cfg.getGenericProgramPoint());
				}

				results.put(cfg, Optional
						.of(cfg.fixpoint(prepared, this, WorkingSet.of(fixpointWorkingSet), wideningThreshold)));
			} catch (SemanticException | AnalysisSetupException e) {
				throw new FixpointException("Error while creating the entrystate for " + cfg, e);
			}
	}

	@Override
	public Collection<CFGWithAnalysisResults<A, H, V, T>> getAnalysisResultsOf(CFG cfg) {
		return Collections.singleton(results.get(cfg).orElse(null));
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		OpenCall open = new OpenCall(call.getCFG(), call.getLocation(), call.isInstanceCall(), call.getQualifier(),
				call.getTargetName(), call.getStaticType(), call.getParameters());
		return getAbstractResultOf(open, entryState, parameters, expressions);
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		return policy.apply(call, entryState, parameters);
	}

	@Override
	public void init(Program program, CallGraph callgraph, OpenCallPolicy policy)
			throws InterproceduralAnalysisException {
		this.program = program;
		this.policy = policy;
	}

	@Override
	public Call resolve(UnresolvedCall call) throws CallResolutionException {
		OpenCall open = new OpenCall(call.getCFG(), call.getLocation(), call.isInstanceCall(), call.getQualifier(),
				call.getTargetName(), call.getStaticType(), call.getParameters());
		return open;
	}
}
