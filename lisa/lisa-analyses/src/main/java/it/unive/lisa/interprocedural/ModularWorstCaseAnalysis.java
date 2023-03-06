package it.unive.lisa.interprocedural;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.FallbackImplementation;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Application;
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
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;

/**
 * A worst case modular analysis were all cfg calls are treated as open calls.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 * @param <T> the type domain
 */
@FallbackImplementation
public class ModularWorstCaseAnalysis<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>> implements InterproceduralAnalysis<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(ModularWorstCaseAnalysis.class);

	/**
	 * The application.
	 */
	private Application app;

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
	private final Map<CFG, Optional<AnalyzedCFG<A, H, V, T>>> results;

	/**
	 * Builds the interprocedural analysis.
	 */
	public ModularWorstCaseAnalysis() {
		this.results = new ConcurrentHashMap<>();
	}

	@Override
	public void fixpoint(AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			FixpointConfiguration conf)
			throws FixpointException {
		for (CFG cfg : IterationLogger.iterate(LOG, app.getAllCFGs(), "Computing fixpoint over the whole program",
				"cfgs"))
			try {
				AnalysisState<A, H, V, T> prepared = entryState;

				for (Parameter arg : cfg.getDescriptor().getFormals()) {
					Variable id = new Variable(arg.getStaticType(), arg.getName(), arg.getAnnotations(),
							arg.getLocation());
					prepared = prepared.assign(id, new PushAny(arg.getStaticType(), arg.getLocation()),
							cfg.getGenericProgramPoint());
				}

				results.put(cfg, Optional.of(cfg.fixpoint(prepared, this, WorkingSet.of(fixpointWorkingSet), conf)));
			} catch (SemanticException | AnalysisSetupException e) {
				throw new FixpointException("Error while creating the entrystate for " + cfg, e);
			}
	}

	@Override
	public Collection<AnalyzedCFG<A, H, V, T>> getAnalysisResultsOf(CFG cfg) {
		return Collections.singleton(results.get(cfg).orElse(null));
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		OpenCall open = new OpenCall(call.getCFG(), call.getLocation(), call.getCallType(), call.getQualifier(),
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
	public void init(Application app, CallGraph callgraph, OpenCallPolicy policy)
			throws InterproceduralAnalysisException {
		this.app = app;
		this.policy = policy;
	}

	@Override
	public Call resolve(UnresolvedCall call, Set<Type>[] types, SymbolAliasing aliasing)
			throws CallResolutionException {
		return new OpenCall(call);
	}
}
