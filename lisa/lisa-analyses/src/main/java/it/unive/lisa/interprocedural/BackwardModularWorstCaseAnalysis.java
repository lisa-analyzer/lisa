package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A worst case modular analysis were all cfg calls are treated as open calls.
 * 
 * @param <A> the {@link AbstractState} of the analysis
 */
public class BackwardModularWorstCaseAnalysis<A extends AbstractState<A>> implements InterproceduralAnalysis<A> {

	private static final Logger LOG = LogManager.getLogger(BackwardModularWorstCaseAnalysis.class);

	private static final ScopeId ID = new UniqueScope();

	/**
	 * The application.
	 */
	private Application app;

	/**
	 * The policy used for computing the result of cfg calls.
	 */
	private OpenCallPolicy policy;

	/**
	 * The cash of the fixpoints' results.
	 */
	private FixpointResults<A> results;

	/**
	 * Builds the interprocedural analysis.
	 */
	public BackwardModularWorstCaseAnalysis() {
	}

	@Override
	public boolean needsCallGraph() {
		return false;
	}

	@Override
	public void fixpoint(
			AnalysisState<A> entryState,
			FixpointConfiguration conf)
			throws FixpointException {
		if (conf.optimize)
			LOG.warn("Optimizations are turned on: this feature is experimental with backward analyses");

		// new fixpoint iteration: restart
		this.results = null;

		Collection<CFG> all = new TreeSet<>((
				c1,
				c2) -> c1.getDescriptor().getLocation()
						.compareTo(c2.getDescriptor().getLocation()));
		all.addAll(app.getAllCFGs());

		for (CFG cfg : IterationLogger.iterate(LOG, all, "Computing fixpoint over the whole program",
				"cfgs"))
			try {
				AnalysisState<A> st = entryState.bottom();

				if (results == null) {
					AnalyzedCFG<A> graph = conf.optimize
							? new OptimizedAnalyzedCFG<>(cfg, ID, st, this)
							: new AnalyzedCFG<>(cfg, ID, entryState);
					CFGResults<A> value = new CFGResults<>(graph);
					this.results = new FixpointResults<>(value.top());
				}

				results.putResult(cfg, ID, cfg.backwardFixpoint(
						entryState,
						this,
						WorkingSet.of(conf.fixpointWorkingSet),
						conf,
						ID));
			} catch (SemanticException e) {
				throw new FixpointException("Error while creating the entrystate for " + cfg, e);
			}
	}

	@Override
	public Collection<AnalyzedCFG<A>> getAnalysisResultsOf(
			CFG cfg) {
		return results.getState(cfg).getAll();
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		OpenCall open = new OpenCall(call.getCFG(), call.getLocation(), call.getCallType(), call.getQualifier(),
				call.getTargetName(), call.getStaticType(), call.getParameters());
		return getAbstractResultOf(open, entryState, parameters, expressions);
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		return policy.apply(call, entryState, parameters);
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy)
			throws InterproceduralAnalysisException {
		this.app = app;
		this.policy = policy;
		this.results = null;
	}

	@Override
	public Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		return new OpenCall(call);
	}

	@Override
	public FixpointResults<A> getFixpointResults() {
		return results;
	}
}
