package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
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
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.VariableRef;
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
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class ModularWorstCaseAnalysis<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		implements
		InterproceduralAnalysis<A, D> {

	private static final Logger LOG = LogManager.getLogger(ModularWorstCaseAnalysis.class);

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
	 * The analysis that is being run.
	 */
	private Analysis<A, D> analysis;

	/**
	 * Builds the interprocedural analysis.
	 */
	public ModularWorstCaseAnalysis() {
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
		// new fixpoint iteration: restart
		this.results = null;

		Collection<CFG> all = new TreeSet<>(ModularWorstCaseAnalysis::sorter);
		all.addAll(app.getAllCFGs());

		for (CFG cfg : IterationLogger.iterate(LOG, all, "Computing fixpoint over the whole program", "cfgs"))
			try {
				AnalysisState<A> st = entryState.bottom();
				StatementStore<A> store = new StatementStore<>(st);

				if (results == null) {
					AnalyzedCFG<A> graph = conf.optimize
							? new OptimizedAnalyzedCFG<>(cfg, ID, st, this)
							: new AnalyzedCFG<>(cfg, ID, entryState);
					CFGResults<A> value = new CFGResults<>(graph);
					this.results = new FixpointResults<>(value.top());
				}

				AnalysisState<A> prepared = entryState;
				for (Parameter arg : cfg.getDescriptor().getFormals()) {
					CodeLocation loc = arg.getLocation();
					Assignment a = new Assignment(
							cfg,
							loc,
							new VariableRef(cfg, loc, arg.getName()),
							arg.getStaticType().unknownValue(cfg, loc));
					prepared = a.forwardSemantics(prepared, this, store);
				}

				results
						.putResult(cfg, ID,
								cfg.fixpoint(prepared, this, WorkingSet.of(conf.fixpointWorkingSet), conf, ID));
			} catch (SemanticException e) {
				throw new FixpointException(
						"Error while creating the entrystate for " + cfg,
						e);
			}
	}

	/**
	 * Sorts two CFGs based on their location, and if they have the same
	 * location, based on their signature.
	 * 
	 * @param g1 the first CFG
	 * @param g2 the second CFG
	 * 
	 * @return a negative integer, zero, or a positive integer as the first
	 *             argument is less than, equal to, or greater than the second
	 *             argument
	 */
	static int sorter(
			CFG g1,
			CFG g2) {
		int cmp = g1.getDescriptor().getLocation().compareTo(g2.getDescriptor().getLocation());
		if (cmp != 0)
			return cmp;

		// they might have been defined at the same location (e.g., synthetic
		// location for generated code). But if they also have the same
		// signature, then they should be equal...
		return g1
				.getDescriptor()
				.getFullSignatureWithParNames()
				.compareTo(g2.getDescriptor().getFullSignatureWithParNames());
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
		OpenCall open = new OpenCall(
				call.getCFG(),
				call.getLocation(),
				call.getCallType(),
				call.getQualifier(),
				call.getTargetName(),
				call.getStaticType(),
				call.getParameters());
		return getAbstractResultOf(open, entryState, parameters, expressions);
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			OpenCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		return policy.apply(call, entryState, analysis, parameters);
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		this.app = app;
		this.policy = policy;
		this.results = null;
		this.analysis = analysis;
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

	@Override
	public Analysis<A, D> getAnalysis() {
		return analysis;
	}

}
