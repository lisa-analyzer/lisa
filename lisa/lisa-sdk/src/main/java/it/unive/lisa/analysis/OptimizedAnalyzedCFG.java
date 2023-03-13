package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.AscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.OptimizedFixpoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An {@link AnalyzedCFG} that has been built using an
 * {@link OptimizedFixpoint}. This means that this graph will only contain
 * results for widening points (that is, {@link Statement}s that part of
 * {@link #getCycleEntries()}), exit statements (that is, {@link Statement}s
 * such that {@link Statement#stopsExecution()} holds), and hotspots (that is,
 * {@link Statement}s such that {@link LiSAConfiguration#hotspots} holds).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 * @param <T> the type of {@link TypeDomain} embedded into the computed abstract
 *                state
 */
public class OptimizedAnalyzedCFG<
		A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends AnalyzedCFG<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(OptimizedAnalyzedCFG.class);

	private final InterproceduralAnalysis<A, H, V, T> interprocedural;

	private StatementStore<A, H, V, T> expanded;

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg             the original control flow graph
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced
	 * @param singleton       an instance of the {@link AnalysisState}
	 *                            containing the abstract state of the analysis
	 *                            that was executed, used to retrieve top and
	 *                            bottom values
	 * @param interprocedural the analysis that have been used to produce this
	 *                            result, and that can be used to unwind the
	 *                            results
	 */
	public OptimizedAnalyzedCFG(CFG cfg,
			ScopeId id,
			AnalysisState<A, H, V, T> singleton,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(cfg, id, singleton);
		this.interprocedural = interprocedural;
	}

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg             the original control flow graph
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced
	 * @param singleton       an instance of the {@link AnalysisState}
	 *                            containing the abstract state of the analysis
	 *                            that was executed, used to retrieve top and
	 *                            bottom values
	 * @param entryStates     the entry state for each entry point of the cfg
	 * @param results         the results of the fixpoint computation
	 * @param interprocedural the analysis that have been used to produce this
	 *                            result, and that can be used to unwind the
	 *                            results
	 */
	public OptimizedAnalyzedCFG(CFG cfg,
			ScopeId id,
			AnalysisState<A, H, V, T> singleton,
			Map<Statement, AnalysisState<A, H, V, T>> entryStates,
			Map<Statement, AnalysisState<A, H, V, T>> results,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(cfg, id, singleton, entryStates, results);
		this.interprocedural = interprocedural;
	}

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg             the original control flow graph
	 * @param id              a {@link ScopeId} meant to identify this specific
	 *                            result based on how it has been produced
	 * @param entryStates     the entry state for each entry point of the cfg
	 * @param results         the results of the fixpoint computation
	 * @param interprocedural the analysis that have been used to produce this
	 *                            result, and that can be used to unwind the
	 *                            results
	 */
	public OptimizedAnalyzedCFG(CFG cfg,
			ScopeId id,
			StatementStore<A, H, V, T> entryStates,
			StatementStore<A, H, V, T> results,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(cfg, id, entryStates, results);
		this.interprocedural = interprocedural;
	}

	private OptimizedAnalyzedCFG(CFG cfg,
			ScopeId id,
			StatementStore<A, H, V, T> entryStates,
			StatementStore<A, H, V, T> results,
			StatementStore<A, H, V, T> expanded,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(cfg, id, entryStates, results);
		this.interprocedural = interprocedural;
		this.expanded = expanded;
	}

	/**
	 * Yields the computed result at a given statement (exit state). If such a
	 * state is not available as it was discarded due to optimization, and
	 * fixpoint's results have not been unwinded yet, a fixpoint iteration is
	 * executed in-place.
	 *
	 * @param st the statement
	 *
	 * @return the result computed at the given statement
	 */
	public AnalysisState<A, H, V, T> getUnwindedAnalysisStateAfter(Statement st) {
		if (results.getKeys().contains(st))
			return results.getState(st);

		if (expanded != null)
			return expanded.getState(st);

		AnalysisState<A, H, V, T> bottom = results.lattice.bottom();
		StatementStore<A, H, V, T> bot = new StatementStore<>(bottom);
		Map<Statement, CompoundState<A, H, V, T>> starting = new HashMap<>();
		for (Entry<Statement, AnalysisState<A, H, V, T>> entry : entryStates)
			starting.put(entry.getKey(), CompoundState.of(entry.getValue(), bot));

		Map<Statement, CompoundState<A, H, V, T>> existing = new HashMap<>();
		CompoundState<A, H, V, T> fallback = CompoundState.of(bottom, bot);
		for (Entry<Statement, AnalysisState<A, H, V, T>> entry : results) {
			Statement stmt = entry.getKey();
			AnalysisState<A, H, V, T> approx = entry.getValue();
			CompoundState<A, H, V, T> def = existing.getOrDefault(stmt, fallback);

			if (!(stmt instanceof Expression) || ((Expression) stmt).getRootStatement() == stmt)
				existing.put(stmt, CompoundState.of(approx, def.intermediateStates));
			else {
				Expression e = (Expression) stmt;
				CompoundState<A, H, V, T> val = existing.computeIfAbsent(e.getRootStatement(),
						ex -> CompoundState.of(bottom, bot.bottom()));
				val.intermediateStates.put(e, approx);
			}
		}

		AscendingFixpoint<A, H, V, T> asc = new AscendingFixpoint<>(this, 0, new PrecomputedAnalysis());
		Fixpoint<CFG, Statement, Edge, CompoundState<A, H, V, T>> fix = new Fixpoint<>(this, true);
		TimerLogger.execAction(LOG, "Unwinding optimizied results of " + this, () -> {
			try {
				Map<Statement, CompoundState<A, H, V, T>> res = fix.fixpoint(
						starting,
						FIFOWorkingSet.mk(),
						asc,
						existing);
				expanded = new StatementStore<>(bottom);
				for (Entry<Statement, CompoundState<A, H, V, T>> e : res.entrySet()) {
					expanded.put(e.getKey(), e.getValue().postState);
					for (Entry<Statement, AnalysisState<A, H, V, T>> ee : e.getValue().intermediateStates)
						expanded.put(ee.getKey(), ee.getValue());
				}
			} catch (FixpointException e) {
				LOG.error("Unable to unwind optimized results of " + this, e);
			}
		});

		return expanded.getState(st);
	}

	private class PrecomputedAnalysis implements InterproceduralAnalysis<A, H, V, T> {

		@Override
		public void init(Application app,
				CallGraph callgraph,
				OpenCallPolicy policy)
				throws InterproceduralAnalysisException {
		}

		@Override
		public void fixpoint(AnalysisState<A, H, V, T> entryState,
				Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
				FixpointConfiguration conf)
				throws FixpointException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<AnalyzedCFG<A, H, V, T>> getAnalysisResultsOf(CFG cfg) {
			return interprocedural.getAnalysisResultsOf(cfg);
		}

		@Override
		public AnalysisState<A, H, V, T> getAbstractResultOf(
				CFGCall call,
				AnalysisState<A, H, V, T> entryState,
				ExpressionSet<SymbolicExpression>[] parameters,
				StatementStore<A, H, V, T> expressions)
				throws SemanticException {
			FixpointResults<A, H, V, T> precomputed = interprocedural.getFixpointResults();
			ScopeToken scope = new ScopeToken(call);
			ScopeId id = getId().push(scope);
			AnalysisState<A, H, V, T> state = entryState.bottom();
			for (CFG target : call.getTargetedCFGs()) {
				AnalysisState<A, H, V, T> res = precomputed.getState(target).getState(id).getExitState();

				// store the return value of the call inside the meta variable
				AnalysisState<A, H, V, T> tmp = entryState.bottom();
				Identifier meta = (Identifier) call.getMetaVariable().pushScope(scope);
				for (SymbolicExpression ret : res.getComputedExpressions())
					tmp = tmp.lub(res.assign(meta, ret, call));

				// save the resulting state
				state = state.lub(tmp.popScope(scope));
			}
			return state;
		}

		@Override
		public AnalysisState<A, H, V, T> getAbstractResultOf(OpenCall call, AnalysisState<A, H, V, T> entryState,
				ExpressionSet<SymbolicExpression>[] parameters, StatementStore<A, H, V, T> expressions)
				throws SemanticException {
			return interprocedural.getAbstractResultOf(call, entryState, parameters, expressions);
		}

		@Override
		public Call resolve(UnresolvedCall call, Set<Type>[] types, SymbolAliasing aliasing)
				throws CallResolutionException {
			return interprocedural.resolve(call, types, aliasing);
		}

		@Override
		public FixpointResults<A, H, V, T> getFixpointResults() {
			return interprocedural.getFixpointResults();
		}
	}

	@Override
	public OptimizedAnalyzedCFG<A, H, V, T> lubAux(AnalyzedCFG<A, H, V, T> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other)
				|| !(other instanceof OptimizedAnalyzedCFG<?, ?, ?, ?>))
			throw new SemanticException(CANNOT_LUB_ERROR);

		OptimizedAnalyzedCFG<A, H, V, T> o = (OptimizedAnalyzedCFG<A, H, V, T>) other;
		return new OptimizedAnalyzedCFG<A, H, V, T>(
				this,
				id,
				entryStates.lub(other.entryStates),
				results.lub(other.results),
				expanded == null ? o.expanded : expanded.lub(o.expanded),
				interprocedural);
	}

	@Override
	public OptimizedAnalyzedCFG<A, H, V, T> glbAux(AnalyzedCFG<A, H, V, T> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other)
				|| !(other instanceof OptimizedAnalyzedCFG<?, ?, ?, ?>))
			throw new SemanticException(CANNOT_GLB_ERROR);

		OptimizedAnalyzedCFG<A, H, V, T> o = (OptimizedAnalyzedCFG<A, H, V, T>) other;
		return new OptimizedAnalyzedCFG<A, H, V, T>(
				this,
				id,
				entryStates.glb(other.entryStates),
				results.glb(other.results),
				expanded == null ? o.expanded : expanded.glb(o.expanded),
				interprocedural);
	}

	@Override
	public OptimizedAnalyzedCFG<A, H, V, T> wideningAux(AnalyzedCFG<A, H, V, T> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other)
				|| !(other instanceof OptimizedAnalyzedCFG<?, ?, ?, ?>))
			throw new SemanticException(CANNOT_WIDEN_ERROR);

		OptimizedAnalyzedCFG<A, H, V, T> o = (OptimizedAnalyzedCFG<A, H, V, T>) other;
		return new OptimizedAnalyzedCFG<A, H, V, T>(
				this,
				id,
				entryStates.widening(other.entryStates),
				results.widening(other.results),
				expanded == null ? o.expanded : expanded.widening(o.expanded),
				interprocedural);
	}

	@Override
	public OptimizedAnalyzedCFG<A, H, V, T> narrowingAux(AnalyzedCFG<A, H, V, T> other) throws SemanticException {
		if (!getDescriptor().equals(other.getDescriptor()) || !sameIDs(other)
				|| !(other instanceof OptimizedAnalyzedCFG<?, ?, ?, ?>))
			throw new SemanticException(CANNOT_NARROW_ERROR);

		OptimizedAnalyzedCFG<A, H, V, T> o = (OptimizedAnalyzedCFG<A, H, V, T>) other;
		return new OptimizedAnalyzedCFG<A, H, V, T>(
				this,
				id,
				entryStates.narrowing(other.entryStates),
				results.narrowing(other.results),
				expanded == null ? o.expanded : expanded.narrowing(o.expanded),
				interprocedural);
	}

	@Override
	public OptimizedAnalyzedCFG<A, H, V, T> top() {
		return new OptimizedAnalyzedCFG<>(this, id.startingId(), entryStates.top(), results.top(), null, null);
	}

	@Override
	public OptimizedAnalyzedCFG<A, H, V, T> bottom() {
		return new OptimizedAnalyzedCFG<>(this, id.startingId(), entryStates.bottom(), results.bottom(), null, null);
	}
}
